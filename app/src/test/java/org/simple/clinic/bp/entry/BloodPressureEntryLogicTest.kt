package org.simple.clinic.bp.entry

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxkotlin.ofType
import io.reactivex.subjects.PublishSubject
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.simple.clinic.TestData
import org.simple.clinic.bp.BloodPressureMeasurement
import org.simple.clinic.bp.BloodPressureReading
import org.simple.clinic.bp.BloodPressureRepository
import org.simple.clinic.bp.ValidationResult
import org.simple.clinic.bp.ValidationResult.ErrorDiastolicEmpty
import org.simple.clinic.bp.ValidationResult.ErrorDiastolicTooHigh
import org.simple.clinic.bp.ValidationResult.ErrorDiastolicTooLow
import org.simple.clinic.bp.ValidationResult.ErrorSystolicEmpty
import org.simple.clinic.bp.ValidationResult.ErrorSystolicLessThanDiastolic
import org.simple.clinic.bp.ValidationResult.ErrorSystolicTooHigh
import org.simple.clinic.bp.ValidationResult.ErrorSystolicTooLow
import org.simple.clinic.bp.entry.BloodPressureEntrySheet.ScreenType.BP_ENTRY
import org.simple.clinic.bp.entry.BloodPressureEntrySheet.ScreenType.DATE_ENTRY
import org.simple.clinic.bp.entry.OpenAs.New
import org.simple.clinic.bp.entry.OpenAs.Update
import org.simple.clinic.facility.Facility
import org.simple.clinic.overdue.AppointmentRepository
import org.simple.clinic.patient.PatientRepository
import org.simple.clinic.user.User
import org.simple.clinic.util.RxErrorsRule
import org.simple.clinic.util.TestUserClock
import org.simple.clinic.util.UserInputDatePaddingCharacter
import org.simple.clinic.util.scheduler.TestSchedulersProvider
import org.simple.clinic.util.toLocalDateAtZone
import org.simple.clinic.util.toUtcInstant
import org.simple.clinic.uuid.FakeUuidGenerator
import org.simple.clinic.widgets.UiEvent
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator.Result.Invalid.InvalidPattern
import org.simple.mobius.migration.MobiusTestFixture
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

typealias Ui = BloodPressureEntryUi
typealias UiChange = (Ui) -> Unit

@RunWith(JUnitParamsRunner::class)
class BloodPressureEntrySheetLogicTest {

  @get:Rule
  val rxErrorsRule = RxErrorsRule()

  private val ui = mock<BloodPressureEntryUi>()
  private val bloodPressureRepository = mock<BloodPressureRepository>()
  private val appointmentRepository = mock<AppointmentRepository>()
  private val patientRepository = mock<PatientRepository>()
  private val testUserClock = TestUserClock()
  private val dateValidator = UserInputDateValidator(testUserClock, DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH))

  private val uiEvents = PublishSubject.create<UiEvent>()
  private val patientUuid = UUID.fromString("79145baf-7a5c-4442-ab30-2da564a32944")

  private val facility = TestData.facility(uuid = UUID.fromString("2a70f82e-92c6-4fce-b60e-6f083a8e725b"))
  private val user = TestData.loggedInUser(
      uuid = UUID.fromString("1367a583-12b1-48c6-ae9d-fb34f9aac449"),
      registrationFacilityUuid = facility.uuid,
      currentFacilityUuid = facility.uuid
  )
  private val measurementUuid = UUID.fromString("25fcfb8b-af3e-40ae-a868-41bd92583f5f")

  private val uiRenderer = BloodPressureEntryUiRenderer(ui)
  private lateinit var fixture: MobiusTestFixture<BloodPressureEntryModel, BloodPressureEntryEvent, BloodPressureEntryEffect>

  @Test
  @Parameters(value = ["90", "120", "300"])
  fun `when valid systolic value is entered, move cursor to diastolic field`(sampleSystolicBp: String) {
    sheetCreatedForNew(patientUuid)
    uiEvents.onNext(SystolicChanged(sampleSystolicBp))
    uiEvents.onNext(SystolicChanged(""))
    uiEvents.onNext(SystolicChanged(sampleSystolicBp))

    verify(ui, times(2)).changeFocusToDiastolic()
  }

  @Test
  @Parameters(value = ["66", "44"])
  fun `when invalid systolic value is entered, don't move cursor to diastolic field`(
      sampleSystolicBp: String
  ) {
    sheetCreatedForNew(patientUuid)
    uiEvents.onNext(SystolicChanged(sampleSystolicBp))
    uiEvents.onNext(SystolicChanged(""))
    uiEvents.onNext(SystolicChanged(sampleSystolicBp))

    verify(ui, never()).changeFocusToDiastolic()
  }

  @Test
  @Parameters(value = [
    "170, 17",
    "17, 1",
    "1,",
    ","])
  fun `when backspace is pressed in diastolic field and it's text is empty, move cursor to systolic and delete last digit`(
      existingSystolic: String,
      systolicAfterBackspace: String
  ) {
    sheetCreatedForNew(patientUuid)
    reset(ui, patientRepository, appointmentRepository, bloodPressureRepository)

    uiEvents.onNext(SystolicChanged(existingSystolic))
    uiEvents.onNext(DiastolicChanged("142"))

    uiEvents.onNext(DiastolicBackspaceClicked)
    uiEvents.onNext(DiastolicChanged("14"))

    uiEvents.onNext(DiastolicBackspaceClicked)
    uiEvents.onNext(DiastolicChanged("1"))

    uiEvents.onNext(DiastolicBackspaceClicked)
    uiEvents.onNext(DiastolicChanged(""))

    uiEvents.onNext(DiastolicBackspaceClicked)

    verify(ui).changeFocusToSystolic()
    if (systolicAfterBackspace.isNotEmpty()) {
      verify(ui).setSystolic(systolicAfterBackspace)
    }
  }

  @Test
  fun `when systolic or diastolic values change, hide any error message`() {
    sheetCreatedForNew(patientUuid)
    uiEvents.onNext(SystolicChanged("12"))
    uiEvents.onNext(SystolicChanged("120"))
    uiEvents.onNext(SystolicChanged("130"))
    uiEvents.onNext(DiastolicChanged("90"))
    uiEvents.onNext(DiastolicChanged("99"))

    verify(ui, times(5)).hideBpErrorMessage()
  }

  @Test
  @Parameters(value = [
    ",",
    "1,1"
  ])
  fun `when save is clicked, BP entry is active, but input is invalid then blood pressure measurement should not be saved`(
      systolic: String,
      diastolic: String
  ) {
    sheetCreatedForNew(patientUuid)
    uiEvents.onNext(SystolicChanged(systolic))
    uiEvents.onNext(DiastolicChanged(diastolic))
    uiEvents.onNext(SaveClicked)

    verify(bloodPressureRepository, never()).saveMeasurement(any(), any(), any(), any(), any(), any())
    verify(ui, never()).setBpSavedResultAndFinish()
  }

  @Test
  @Parameters(method = "params for OpenAs and bp validation errors")
  fun `when BP entry is active, BP readings are invalid and save is clicked then date entry should not be shown`(
      openAs: OpenAs,
      error: ValidationResult
  ) {
    if (openAs is Update) {
      whenever(bloodPressureRepository.measurementImmediate(openAs.bpUuid)).doReturn(TestData.bloodPressureMeasurement(uuid = openAs.bpUuid))
    }

    sheetCreated(openAs)
    uiEvents.run {
      onNext(ScreenChanged(BP_ENTRY))
      onNext(SystolicChanged(""))
      onNext(DiastolicChanged(""))
      onNext(SaveClicked)
    }

    verify(ui, never()).showDateEntryScreen()
  }

  @Test
  @Parameters(method = "params for prefilling bp measurements")
  fun `when screen is opened to update a blood pressure, the blood pressure must be prefilled`(
      openAs: OpenAs,
      bloodPressureMeasurement: BloodPressureMeasurement?
  ) {
    if (openAs is Update) {
      whenever(bloodPressureRepository.measurementImmediate(any())).doReturn(bloodPressureMeasurement!!)
    }

    sheetCreated(openAs)

    if (openAs is Update) {
      val verify = verify(ui)
      verify.setSystolic(bloodPressureMeasurement!!.reading.systolic.toString())
      verify.setDiastolic(bloodPressureMeasurement.reading.diastolic.toString())
    } else {
      val verify = verify(ui, never())
      verify.setSystolic(any())
      verify.setDiastolic(any())
    }
  }

  @Suppress("Unused")
  private fun `params for prefilling bp measurements`(): List<List<Any?>> {
    val bpUuid = UUID.fromString("8ddd7237-f331-4d34-9ce6-a3cd0c4c8133")
    return listOf(
        listOf(New(patientUuid), null),
        listOf(Update(bpUuid), TestData.bloodPressureMeasurement(uuid = bpUuid, patientUuid = patientUuid))
    )
  }

  @Test
  @Parameters(method = "params for showing remove button")
  fun `the remove BP button must be shown when the sheet is opened for update`(
      openAs: OpenAs,
      shouldShowRemoveBpButton: Boolean
  ) {
    whenever(bloodPressureRepository.measurementImmediate(any())).doReturn(TestData.bloodPressureMeasurement())

    sheetCreated(openAs)

    if (shouldShowRemoveBpButton) {
      verify(ui).showRemoveBpButton()
    } else {
      verify(ui).hideRemoveBpButton()
    }
  }

  @Suppress("Unused")
  private fun `params for showing remove button`(): List<List<Any>> {
    return listOf(
        listOf(New(UUID.fromString("3127284e-8db4-4359-9386-57b8837573e9")), false),
        listOf(Update(UUID.fromString("baac5893-3670-4d9c-a5ff-12405cbb1ad5")), true))
  }

  @Test
  @Parameters(method = "params for setting the title of the sheet")
  fun `the correct title should be shown when the sheet is opened`(
      openAs: OpenAs,
      showEntryTitle: Boolean
  ) {
    whenever(bloodPressureRepository.measurementImmediate(any())).doReturn(TestData.bloodPressureMeasurement())

    sheetCreated(openAs)

    if (showEntryTitle) {
      verify(ui).showEnterNewBloodPressureTitle()
    } else {
      verify(ui).showEditBloodPressureTitle()
    }
  }

  @Suppress("Unused")
  private fun `params for setting the title of the sheet`(): List<List<Any>> {
    return listOf(
        listOf(New(UUID.fromString("1526b550-c7a1-440a-a916-043f959bc6c5")), true),
        listOf(Update(UUID.fromString("66f89f1a-4d13-4491-970d-0d09c9ce4043")), false))
  }

  @Test
  fun `when the remove button is clicked, the confirmation alert must be shown`() {
    val bloodPressure = TestData.bloodPressureMeasurement()
    whenever(bloodPressureRepository.measurementImmediate(any())).doReturn(bloodPressure)

    sheetCreatedForUpdate(bloodPressure.uuid)
    uiEvents.onNext(RemoveBloodPressureClicked)

    verify(ui).showConfirmRemoveBloodPressureDialog(bloodPressure.uuid)
  }

  @Test
  @Parameters(method = "params for checking valid date input")
  fun `when save is clicked, date entry is active, but input is invalid then BP measurement should not be saved`(
      openAs: OpenAs
  ) {
    if (openAs is Update) {
      whenever(bloodPressureRepository.measurementImmediate(openAs.bpUuid)).doReturn(TestData.bloodPressureMeasurement(uuid = openAs.bpUuid))
    }

    sheetCreated(openAs)
    uiEvents.onNext(ScreenChanged(DATE_ENTRY))
    uiEvents.onNext(DayChanged("invalid"))
    uiEvents.onNext(MonthChanged("4"))
    uiEvents.onNext(YearChanged("9"))
    uiEvents.onNext(SaveClicked)

    when (openAs) {
      is New -> verify(bloodPressureRepository, never()).saveMeasurement(any(), any(), any(), any(), any(), any())
      is Update -> verify(bloodPressureRepository, never()).updateMeasurement(any())
      else -> throw AssertionError()
    }

    verify(ui, never()).setBpSavedResultAndFinish()
    verify(ui).showInvalidDateError()
  }

  @Suppress("Unused")
  private fun `params for checking valid date input`(): List<OpenAs> {
    return listOf(
        New(patientUuid),
        Update(UUID.fromString("f6f27cad-8b82-461e-8b1e-e14c2ac63832"))
    )
  }

  @Test
  fun `when save is clicked for a new BP, date entry is active and input is valid then a BP measurement should be saved`() {
    val inputDate = LocalDate.of(1990, 2, 13)
    val entryDateAsInstant = inputDate.toUtcInstant(testUserClock)
    whenever(bloodPressureRepository.saveMeasurement(
        patientUuid = patientUuid,
        reading = BloodPressureReading(130, 110),
        loggedInUser = user,
        currentFacility = facility,
        recordedAt = entryDateAsInstant,
        uuid = measurementUuid)).doReturn(TestData.bloodPressureMeasurement(patientUuid = patientUuid))

    sheetCreatedForNew(patientUuid)
    uiEvents.run {
      onNext(ScreenChanged(BP_ENTRY))
      onNext(SystolicChanged("13"))
      onNext(DiastolicChanged("11"))
      onNext(SystolicChanged("130"))
      onNext(DiastolicChanged("110"))
      onNext(SaveClicked)
      onNext(ScreenChanged(DATE_ENTRY))
      onNext(DayChanged("13"))
      onNext(MonthChanged("02"))
      onNext(YearChanged("1990"))
      onNext(SaveClicked)
    }

    verify(bloodPressureRepository, never()).updateMeasurement(any())
    verify(appointmentRepository).markAppointmentsCreatedBeforeTodayAsVisited(patientUuid)
    verify(patientRepository).compareAndUpdateRecordedAt(patientUuid, entryDateAsInstant)
    verify(ui).setBpSavedResultAndFinish()
  }

  @Test
  fun `when save is clicked while updating a BP, date entry is active and input is valid then the updated BP measurement should be saved`() {
    val oldCreatedAt = LocalDate.of(1990, 1, 13).toUtcInstant(testUserClock)
    val existingBp = TestData.bloodPressureMeasurement(
        userUuid = user.uuid,
        facilityUuid = facility.uuid,
        systolic = 9000,
        diastolic = 8999,
        createdAt = oldCreatedAt,
        updatedAt = oldCreatedAt,
        recordedAt = oldCreatedAt
    )

    val newInputDate = LocalDate.of(1991, 2, 14)
    val newInputDateAsInstant = newInputDate.toUtcInstant(testUserClock)
    whenever(bloodPressureRepository.saveMeasurement(
        patientUuid = patientUuid,
        reading = BloodPressureReading(120, 110),
        loggedInUser = user,
        currentFacility = facility,
        recordedAt = newInputDateAsInstant,
        uuid = measurementUuid)).doReturn(TestData.bloodPressureMeasurement())
    whenever(bloodPressureRepository.measurementImmediate(existingBp.uuid)).doReturn(existingBp)

    sheetCreatedForUpdate(existingBp.uuid)
    uiEvents.run {
      onNext(ScreenChanged(BP_ENTRY))
      onNext(SystolicChanged("120"))
      onNext(DiastolicChanged("110"))
      onNext(SaveClicked)
    }

    uiEvents.run {
      onNext(ScreenChanged(DATE_ENTRY))
      onNext(DayChanged("14"))
      onNext(MonthChanged("02"))
      onNext(YearChanged("1991"))
      onNext(SaveClicked)
    }

    val updatedBp = existingBp.copy(
        reading = BloodPressureReading(120, 110),
        updatedAt = oldCreatedAt,
        recordedAt = newInputDateAsInstant
    )
    verify(bloodPressureRepository).updateMeasurement(updatedBp)
    verify(patientRepository).compareAndUpdateRecordedAt(updatedBp.patientUuid, updatedBp.recordedAt)

    verify(bloodPressureRepository, never()).saveMeasurement(any(), any(), any(), any(), any(), any())
    verify(appointmentRepository, never()).markAppointmentsCreatedBeforeTodayAsVisited(any())
    verify(ui).setBpSavedResultAndFinish()
  }

  @Test
  @Parameters(method = "params for showing date validation errors")
  fun `when save is clicked, date entry is active and input is invalid then validation errors should be shown`(
      testParams: InvalidDateTestParams
  ) {
    val (openAs, day, month, year, errorResult, uiChangeVerification) = testParams

    if (openAs is Update) {
      whenever(bloodPressureRepository.measurementImmediate(openAs.bpUuid)).doReturn(TestData.bloodPressureMeasurement(uuid = openAs.bpUuid))
    }

    // This assertion was written only to stabilize the test by verifying incoming parameters
    assertThat(dateValidator.validate("$day/$month/$year"))
        .isEqualTo(errorResult)

    sheetCreated(openAs)
    uiEvents.run {
      onNext(ScreenChanged(DATE_ENTRY))
      onNext(DayChanged(day))
      onNext(MonthChanged(month))
      onNext(YearChanged(year.takeLast(2)))
      onNext(SaveClicked)
    }

    verify(bloodPressureRepository, never()).saveMeasurement(any(), any(), any(), any(), any(), any())
    verify(bloodPressureRepository, never()).updateMeasurement(any())
    verify(ui, never()).setBpSavedResultAndFinish()

    uiChangeVerification(ui)
  }

  @Suppress("unused")
  fun `params for showing date validation errors`(): List<InvalidDateTestParams> {
    val existingBpUuid = UUID.fromString("2c4eccbb-d1bc-4c7c-b1ec-60a13acfeea4")
    return listOf(
        InvalidDateTestParams(New(patientUuid), "-invalid-", "-invalid-", "-invalid-", InvalidPattern) { ui: Ui -> verify(ui).showInvalidDateError() },
        InvalidDateTestParams(Update(existingBpUuid), "-invalid-", "-invalid-", "-invalid-", InvalidPattern) { ui: Ui -> verify(ui).showInvalidDateError() })
  }

  data class InvalidDateTestParams(
      val openAs: OpenAs,
      val day: String,
      val month: String,
      val year: String,
      val errorResult: UserInputDateValidator.Result,
      val uiChangeVerification: UiChange
  )

  @Test
  fun `when date values change, hide any error message`() {
    sheetCreatedForNew(patientUuid)

    uiEvents.run {
      onNext(DayChanged("14"))
      onNext(MonthChanged("02"))
      onNext(YearChanged("1991"))
    }

    verify(ui, atLeastOnce()).hideDateErrorMessage()
  }

  @Test
  @Parameters(method = "params for OpenAs types")
  fun `when BP entry is active, BP readings are valid and next arrow is pressed then date entry should be shown`(
      openAs: OpenAs
  ) {
    if (openAs is Update) {
      whenever(bloodPressureRepository.measurementImmediate(openAs.bpUuid)).doReturn(TestData.bloodPressureMeasurement(uuid = openAs.bpUuid))
    }

    sheetCreated(openAs)
    uiEvents.run {
      onNext(ScreenChanged(BP_ENTRY))
      onNext(SystolicChanged("120"))
      onNext(DiastolicChanged("110"))
      onNext(ChangeDateClicked)
    }

    verify(ui).showDateEntryScreen()
  }

  @Suppress("unused")
  fun `params for OpenAs and bp validation errors`(): List<Any> {
    val bpUuid = UUID.fromString("99fed5e5-19a8-4ece-9d07-6beab70ee77c")
    return listOf(
        listOf(New(patientUuid), ErrorSystolicEmpty),
        listOf(New(patientUuid), ErrorDiastolicEmpty),
        listOf(New(patientUuid), ErrorSystolicTooHigh),
        listOf(New(patientUuid), ErrorSystolicTooLow),
        listOf(New(patientUuid), ErrorDiastolicTooHigh),
        listOf(New(patientUuid), ErrorDiastolicTooLow),
        listOf(New(patientUuid), ErrorSystolicLessThanDiastolic),
        listOf(Update(bpUuid), ErrorSystolicEmpty),
        listOf(Update(bpUuid), ErrorDiastolicEmpty),
        listOf(Update(bpUuid), ErrorSystolicTooHigh),
        listOf(Update(bpUuid), ErrorSystolicTooLow),
        listOf(Update(bpUuid), ErrorDiastolicTooHigh),
        listOf(Update(bpUuid), ErrorDiastolicTooLow),
        listOf(Update(bpUuid), ErrorSystolicLessThanDiastolic))
  }

  @Test
  fun `when BP entry is active and back is pressed then the sheet should be closed`() {
    sheetCreatedForNew(patientUuid)

    uiEvents.run {
      onNext(ScreenChanged(BP_ENTRY))
      onNext(BackPressed)
    }

    verify(ui).dismiss()
  }

  @Test
  fun `when BP entry is active and BP readings are invalid and blood pressure date is clicked, then show BP validation errors`() {
    sheetCreatedForNew(patientUuid)
    uiEvents.run {
      onNext(ScreenChanged(BP_ENTRY))
      onNext(SystolicChanged(""))
      onNext(DiastolicChanged("80"))
      onNext(ChangeDateClicked)
    }

    verify(ui).showSystolicEmptyError()
  }

  @Test
  fun `when screen is opened for a new BP, then the date should be prefilled with the current date`() {
    val currentDate = LocalDate.of(2018, 4, 23)
    testUserClock.setDate(currentDate, UTC)

    sheetCreatedForNew(patientUuid)

    verify(ui).setDateOnInputFields(currentDate)
  }

  @Test
  fun `when screen is opened for updating an existing BP, then the date should be prefilled with the BP's recorded date`() {
    val recordedAtDate = LocalDate.of(2018, 4, 23)
    val recordedAtDateAsInstant = recordedAtDate.atStartOfDay().toInstant(UTC)
    val existingBp = TestData.bloodPressureMeasurement(recordedAt = recordedAtDateAsInstant)

    whenever(bloodPressureRepository.measurementImmediate(existingBp.uuid)).doReturn(existingBp)

    sheetCreatedForUpdate(existingBp.uuid)

    verify(ui, times(1)).setDateOnInputFields(recordedAtDate)
  }

  @Suppress("Unused")
  private fun `params for OpenAs types`(): List<OpenAs> {
    return listOf(
        New(patientUuid),
        Update(UUID.fromString("d9d0050a-7fa1-4293-868a-7640ebc93cd8"))
    )
  }

  @Test
  fun `whenever the BP sheet is shown to add a new BP, then show today's date`() {
    val today = LocalDate.now(testUserClock)

    sheetCreatedForNew(patientUuid)
    uiEvents.onNext(ScreenChanged(BP_ENTRY))

    verify(ui).showBpDate(today)

    verify(ui).setDateOnInputFields(today)
    verify(ui).hideRemoveBpButton()
    verify(ui).showEnterNewBloodPressureTitle()
    verify(ui, times(3)).hideProgress()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `whenever the BP sheet is shown to update an existing BP, then show the BP date`() {
    val bp = TestData.bloodPressureMeasurement(patientUuid = patientUuid)
    val recordedDate = bp.recordedAt.toLocalDateAtZone(testUserClock.zone)
    whenever(bloodPressureRepository.measurementImmediate(any())).doReturn(bp)

    sheetCreatedForUpdate(bp.uuid)
    uiEvents.onNext(ScreenChanged(BP_ENTRY))

    verify(ui).showBpDate(recordedDate)

    verify(ui).setDateOnInputFields(recordedDate)
    verify(ui).setSystolic(bp.reading.systolic.toString())
    verify(ui).setDiastolic(bp.reading.diastolic.toString())
    verify(ui).showRemoveBpButton()
    verify(ui).showEditBloodPressureTitle()
    verify(ui, times(4)).hideProgress()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `whenever the BP sheet has an invalid date (new BP) and show BP button is pressed, then show date validation errors`() {
    val systolic = 120.toString()
    val diastolic = 110.toString()

    sheetCreatedForNew(patientUuid)
    with(uiEvents) {
      onNext(ScreenChanged(BP_ENTRY))
      onNext(SystolicChanged(systolic))
      onNext(DiastolicChanged(diastolic))
      onNext(ChangeDateClicked)
      onNext(ScreenChanged(DATE_ENTRY))
      onNext(DayChanged("1"))
      onNext(MonthChanged("24"))
      onNext(YearChanged("91"))

      reset(ui)
      onNext(ShowBpClicked)
    }

    verify(ui).showInvalidDateError()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `whenever the BP sheet has a valid date (new BP) and show BP button is pressed, then show bp entry sheet`() {
    val systolic = 120.toString()
    val diastolic = 110.toString()
    val bpDate = LocalDate.of(1991, 12, 1)

    sheetCreatedForNew(patientUuid)
    with(uiEvents) {
      onNext(ScreenChanged(BP_ENTRY))
      onNext(SystolicChanged(systolic))
      onNext(DiastolicChanged(diastolic))
      onNext(ChangeDateClicked)
      onNext(ScreenChanged(DATE_ENTRY))
      onNext(DayChanged(bpDate.dayOfMonth.toString()))
      onNext(MonthChanged(bpDate.monthValue.toString()))
      onNext(YearChanged(bpDate.year.toString()))

      reset(ui)
      onNext(ShowBpClicked)
    }

    verify(ui).showBpEntryScreen()
    verify(ui).showBpDate(bpDate)
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `whenever the BP sheet has an invalid date (new BP) and back key is pressed, then show date validation errors`() {
    val systolic = 120.toString()
    val diastolic = 110.toString()

    sheetCreatedForNew(patientUuid)
    with(uiEvents) {
      onNext(ScreenChanged(BP_ENTRY))
      onNext(SystolicChanged(systolic))
      onNext(DiastolicChanged(diastolic))
      onNext(ChangeDateClicked)
      onNext(ScreenChanged(DATE_ENTRY))
      onNext(DayChanged("1"))
      onNext(MonthChanged("24"))
      onNext(YearChanged("91"))

      reset(ui)
      onNext(BackPressed)
    }

    verify(ui).showInvalidDateError()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `when the update BP sheet has an invalid date and show BP button is pressed, then show date validation errors`() {
    val systolic = 120.toString()
    val diastolic = 110.toString()

    val bp = TestData.bloodPressureMeasurement(patientUuid = patientUuid)
    whenever(bloodPressureRepository.measurementImmediate(any())).doReturn(bp)

    sheetCreatedForUpdate(bp.uuid)
    with(uiEvents) {
      onNext(ScreenChanged(BP_ENTRY))
      onNext(SystolicChanged(systolic))
      onNext(DiastolicChanged(diastolic))
      onNext(ChangeDateClicked)
      onNext(ScreenChanged(DATE_ENTRY))
      onNext(DayChanged("1"))
      onNext(MonthChanged("24"))
      onNext(YearChanged("91"))

      reset(ui)
      onNext(ShowBpClicked)
    }

    verify(ui).showInvalidDateError()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `when the update BP sheet has an invalid date and back key is pressed, then show date validation errors`() {
    val systolic = 120.toString()
    val diastolic = 110.toString()

    val bp = TestData.bloodPressureMeasurement(patientUuid = patientUuid)
    whenever(bloodPressureRepository.measurementImmediate(any())).doReturn(bp)

    sheetCreatedForUpdate(bp.uuid)
    with(uiEvents) {
      onNext(ScreenChanged(BP_ENTRY))
      onNext(SystolicChanged(systolic))
      onNext(DiastolicChanged(diastolic))
      onNext(ChangeDateClicked)
      onNext(ScreenChanged(DATE_ENTRY))
      onNext(DayChanged("1"))
      onNext(MonthChanged("24"))
      onNext(YearChanged("91"))

      reset(ui)
      onNext(BackPressed)
    }

    verify(ui).showInvalidDateError()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `when the data entry sheet changes date and back button is pressed, then update date button in BP entry`() {
    val systolic = 120.toString()
    val diastolic = 110.toString()
    val localDate = LocalDate.of(1916, 5, 10)

    sheetCreatedForNew(patientUuid)
    with(uiEvents) {
      onNext(ScreenChanged(BP_ENTRY))
      onNext(SystolicChanged(systolic))
      onNext(DiastolicChanged(diastolic))
      onNext(ChangeDateClicked)
      onNext(ScreenChanged(DATE_ENTRY))
      onNext(DayChanged(localDate.dayOfMonth.toString()))
      onNext(MonthChanged(localDate.monthValue.toString()))
      onNext(YearChanged(localDate.year.toString()))

      reset(ui)
      onNext(BackPressed)
    }

    verify(ui).showBpDate(localDate)
    verify(ui).showBpEntryScreen()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `when the data entry sheet changes date and show BP button is pressed, then update date button in BP entry`() {
    val systolic = 120.toString()
    val diastolic = 110.toString()
    val localDate = LocalDate.of(1916, 5, 10)

    sheetCreatedForNew(patientUuid)
    with(uiEvents) {
      onNext(ScreenChanged(BP_ENTRY))
      onNext(SystolicChanged(systolic))
      onNext(DiastolicChanged(diastolic))
      onNext(ChangeDateClicked)
      onNext(ScreenChanged(DATE_ENTRY))
      onNext(DayChanged(localDate.dayOfMonth.toString()))
      onNext(MonthChanged(localDate.monthValue.toString()))
      onNext(YearChanged(localDate.year.toString()))

      reset(ui)
      onNext(ShowBpClicked)
    }

    verify(ui).showBpDate(localDate)
    verify(ui).showBpEntryScreen()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `when done button is clicked in new BP entry, then save BP with entered date immediately`() {
    val systolic = 120
    val diastolic = 110
    val inputDate = LocalDate.of(1916, 5, 10)
    val entryDateAsInstant = inputDate.toUtcInstant(testUserClock)

    whenever(bloodPressureRepository.saveMeasurement(
        patientUuid = patientUuid,
        reading = BloodPressureReading(systolic, diastolic),
        loggedInUser = user,
        currentFacility = facility,
        recordedAt = entryDateAsInstant,
        uuid = measurementUuid)).doReturn(TestData.bloodPressureMeasurement(patientUuid = patientUuid))

    sheetCreatedForNew(patientUuid)
    with(uiEvents) {
      onNext(ScreenChanged(BP_ENTRY))
      onNext(SystolicChanged(systolic.toString()))
      onNext(DiastolicChanged(diastolic.toString()))
      onNext(DayChanged(inputDate.dayOfMonth.toString()))
      onNext(MonthChanged(inputDate.monthValue.toString()))
      onNext(YearChanged(inputDate.year.toString()))

      reset(ui)
      onNext(SaveClicked)
    }

    verify(appointmentRepository).markAppointmentsCreatedBeforeTodayAsVisited(patientUuid)
    verify(patientRepository).compareAndUpdateRecordedAt(patientUuid, entryDateAsInstant)
    verify(ui).setBpSavedResultAndFinish()
    verify(ui).showProgress()
    verify(ui).hideProgress()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `when done button is clicked in update BP entry, then save BP with entered date immediately`() {
    val systolic = 120.toString()
    val diastolic = 110.toString()
    val createdAt = LocalDate.of(1990, 1, 13).toUtcInstant(testUserClock)
    val existingBp = TestData.bloodPressureMeasurement(
        userUuid = user.uuid,
        facilityUuid = facility.uuid,
        patientUuid = patientUuid,
        systolic = 9000,
        diastolic = 8999,
        createdAt = createdAt,
        updatedAt = createdAt,
        recordedAt = createdAt
    )

    val newInputDate = LocalDate.of(1991, 2, 14)

    whenever(bloodPressureRepository.measurementImmediate(any())).doReturn(existingBp)

    sheetCreatedForUpdate(existingBp.uuid)
    with(uiEvents) {
      onNext(ScreenChanged(BP_ENTRY))
      onNext(SystolicChanged(systolic))
      onNext(DiastolicChanged(diastolic))
      onNext(DayChanged(newInputDate.dayOfMonth.toString()))
      onNext(MonthChanged(newInputDate.monthValue.toString()))
      onNext(YearChanged(newInputDate.year.toString()))

      reset(ui)
      onNext(SaveClicked)
    }

    val entryDateAsInstant = newInputDate.toUtcInstant(testUserClock)
    val newInputDateAsInstant = newInputDate.toUtcInstant(testUserClock)
    val updatedBp = existingBp.copy(
        reading = BloodPressureReading(systolic.toInt(), diastolic.toInt()),
        updatedAt = createdAt,
        recordedAt = newInputDateAsInstant
    )
    verify(bloodPressureRepository).updateMeasurement(updatedBp)

    verify(bloodPressureRepository, never()).saveMeasurement(any(), any(), any(), any(), any(), any())
    verify(appointmentRepository, never()).markAppointmentsCreatedBeforeTodayAsVisited(any())

    verify(patientRepository).compareAndUpdateRecordedAt(patientUuid, entryDateAsInstant)
    verify(ui).setBpSavedResultAndFinish()
    verify(ui).showProgress()
    verify(ui).hideProgress()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `when a different user clicks on save while updating a BP, then the updated BP measurement should be saved with the user's ID and the corresponding facility ID`() {
    // given
    val differentFacility = TestData.facility(uuid = UUID.fromString("f895b54f-ee32-4471-bc0c-a91b80368778"))
    val userFromDifferentFacility = TestData.loggedInUser(
        uuid = UUID.fromString("4844b826-a162-49fe-b92c-962da172e86c"),
        registrationFacilityUuid = differentFacility.uuid,
        currentFacilityUuid = differentFacility.uuid
    )

    val oldCreatedAt = Instant.parse("1990-01-13T00:00:00Z")
    val patientUuid = UUID.fromString("af92b081-0131-4f91-9c28-98da5737945b")
    val existingBp = TestData.bloodPressureMeasurement(
        uuid = patientUuid,
        userUuid = user.uuid,
        facilityUuid = facility.uuid,
        systolic = 9000,
        diastolic = 8999,
        createdAt = oldCreatedAt,
        updatedAt = oldCreatedAt,
        recordedAt = oldCreatedAt
    )

    whenever(bloodPressureRepository.measurementImmediate(existingBp.uuid)).doReturn(existingBp)

    sheetCreatedForUpdate(existingBp.uuid, userFromDifferentFacility, differentFacility)
    uiEvents.run {
      onNext(ScreenChanged(BP_ENTRY))
      onNext(SystolicChanged("120"))
      onNext(DiastolicChanged("110"))
      onNext(SaveClicked)
    }

    uiEvents.run {
      onNext(ScreenChanged(DATE_ENTRY))
      onNext(DayChanged("14"))
      onNext(MonthChanged("02"))
      onNext(YearChanged("1991"))
      onNext(SaveClicked)
    }

    val newInputDate = LocalDate.parse("1991-02-14")
    val newInputDateAsInstant = newInputDate.toUtcInstant(testUserClock)
    val updatedBp = existingBp.copy(
        reading = BloodPressureReading(120, 110),
        updatedAt = oldCreatedAt,
        recordedAt = newInputDateAsInstant,
        userUuid = userFromDifferentFacility.uuid,
        facilityUuid = differentFacility.uuid
    )
    verify(bloodPressureRepository).updateMeasurement(updatedBp)
    verify(patientRepository).compareAndUpdateRecordedAt(updatedBp.patientUuid, updatedBp.recordedAt)

    verify(bloodPressureRepository, never()).saveMeasurement(any(), any(), any(), any(), any(), any())
    verify(appointmentRepository, never()).markAppointmentsCreatedBeforeTodayAsVisited(any())
    verify(ui).setBpSavedResultAndFinish()
  }

  @Test
  fun `when done button is clicked by a user from a different facility in update BP entry, then save BP with entered date immediately`() {
    // given
    val differentFacility = TestData.facility(uuid = UUID.fromString("d9ea6458-fbe2-4d59-b1ac-7dc77b234486"))
    val userFromDifferentFacility = TestData.loggedInUser(
        uuid = UUID.fromString("e246c4fb-5a8d-418a-b80a-9e9d12ca1a8c"),
        currentFacilityUuid = differentFacility.uuid,
        registrationFacilityUuid = differentFacility.uuid
    )

    val systolic = 120.toString()
    val diastolic = 110.toString()
    val createdAt = LocalDate.of(1990, 1, 13).toUtcInstant(testUserClock)
    val existingBp = TestData.bloodPressureMeasurement(
        uuid = UUID.fromString("20d6aba0-80a9-4ab1-b0b6-5261a53f5fe5"),
        userUuid = user.uuid,
        facilityUuid = facility.uuid,
        patientUuid = patientUuid,
        systolic = 9000,
        diastolic = 8999,
        createdAt = createdAt,
        updatedAt = createdAt,
        recordedAt = createdAt
    )

    val newInputDate = LocalDate.of(1991, 2, 14)
    val newInputDateAsInstant = newInputDate.toUtcInstant(testUserClock)
    val updatedBp = existingBp.copy(
        reading = BloodPressureReading(systolic.toInt(), diastolic.toInt()),
        userUuid = userFromDifferentFacility.uuid,
        facilityUuid = differentFacility.uuid,
        recordedAt = newInputDateAsInstant
    )

    whenever(bloodPressureRepository.measurementImmediate(any())).doReturn(existingBp)

    sheetCreatedForUpdate(existingBp.uuid, userFromDifferentFacility, differentFacility)
    with(uiEvents) {
      onNext(ScreenChanged(BP_ENTRY))
      onNext(SystolicChanged(systolic))
      onNext(DiastolicChanged(diastolic))
      onNext(DayChanged(newInputDate.dayOfMonth.toString()))
      onNext(MonthChanged(newInputDate.monthValue.toString()))
      onNext(YearChanged(newInputDate.year.toString()))

      reset(ui)
      onNext(SaveClicked)
    }

    val entryDateAsInstant = newInputDate.toUtcInstant(testUserClock)
    verify(bloodPressureRepository).updateMeasurement(updatedBp)

    verify(bloodPressureRepository, never()).saveMeasurement(any(), any(), any(), any(), any(), any())
    verify(appointmentRepository, never()).markAppointmentsCreatedBeforeTodayAsVisited(any())

    verify(patientRepository).compareAndUpdateRecordedAt(patientUuid, entryDateAsInstant)
    verify(ui).setBpSavedResultAndFinish()
    verify(ui).showProgress()
    verify(ui).hideProgress()
    verifyNoMoreInteractions(ui)
  }

  private fun sheetCreatedForNew(
      patientUuid: UUID,
      user: User = this.user,
      facility: Facility = this.facility
  ) {
    val openAsNew = New(patientUuid)
    instantiateFixture(openAsNew, user, facility)
  }

  private fun sheetCreatedForUpdate(
      existingBpUuid: UUID,
      user: User = this.user,
      facility: Facility = this.facility
  ) {
    val openAsUpdate = Update(existingBpUuid)
    instantiateFixture(openAsUpdate, user, facility)
  }

  private fun sheetCreated(openAs: OpenAs) {
    when (openAs) {
      is New -> sheetCreatedForNew(openAs.patientUuid)
      is Update -> sheetCreatedForUpdate(openAs.bpUuid)
      else -> throw IllegalStateException("Unknown `openAs`: $openAs")
    }
  }

  private fun instantiateFixture(
      openAs: OpenAs,
      user: User,
      facility: Facility
  ) {
    val effectHandler = BloodPressureEntryEffectHandler(
        ui = ui,
        patientRepository = patientRepository,
        bloodPressureRepository = bloodPressureRepository,
        appointmentsRepository = appointmentRepository,
        userClock = testUserClock,
        schedulersProvider = TestSchedulersProvider.trampoline(),
        uuidGenerator = FakeUuidGenerator.fixed(measurementUuid),
        currentUser = { user },
        currentFacility = { facility }
    ).build()

    fixture = MobiusTestFixture(
        uiEvents.ofType(),
        BloodPressureEntryModel.create(openAs, LocalDate.now(testUserClock).year),
        BloodPressureEntryInit(),
        BloodPressureEntryUpdate(dateValidator, LocalDate.now(UTC), UserInputDatePaddingCharacter.ZERO),
        effectHandler,
        uiRenderer::render
    ).also { it.start() }
  }
}
