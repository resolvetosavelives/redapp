package org.simple.clinic.bp.entry

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxkotlin.ofType
import io.reactivex.subjects.PublishSubject
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.simple.clinic.TestData
import org.simple.clinic.bp.BloodPressureRepository
import org.simple.clinic.bp.entry.BloodPressureEntrySheet.ScreenType.BP_ENTRY
import org.simple.clinic.bp.entry.OpenAs.New
import org.simple.clinic.bp.entry.OpenAs.Update
import org.simple.clinic.overdue.AppointmentRepository
import org.simple.clinic.patient.PatientRepository
import org.simple.clinic.util.RxErrorsRule
import org.simple.clinic.util.TestUserClock
import org.simple.clinic.util.UserInputDatePaddingCharacter
import org.simple.clinic.util.scheduler.TestSchedulersProvider
import org.simple.clinic.uuid.FakeUuidGenerator
import org.simple.clinic.widgets.UiEvent
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator
import org.simple.mobius.migration.MobiusTestFixture
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@RunWith(JUnitParamsRunner::class)
class BloodPressureValidationTest {

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
      currentFacilityUuid = facility.uuid,
      registrationFacilityUuid = facility.uuid
  )

  private val uiRenderer = BloodPressureEntryUiRenderer(ui)
  private lateinit var fixture: MobiusTestFixture<BloodPressureEntryModel, BloodPressureEntryEvent, BloodPressureEntryEffect>

  @Test
  @Parameters(method = "params for bp validation errors and expected ui changes")
  fun `when BP entry is active, and BP readings are invalid then show error`(
      testParams: ValidationErrorsAndUiChangesTestParams
  ) {
    val (systolic, diastolic, uiChangeVerification) = testParams

    sheetCreatedForNew(patientUuid)
    uiEvents.onNext(ScreenChanged(BP_ENTRY))
    uiEvents.onNext(SystolicChanged(systolic))
    uiEvents.onNext(DiastolicChanged(diastolic))
    uiEvents.onNext(SaveClicked)

    verify(bloodPressureRepository, never()).saveMeasurement(any(), any(), any(), any(), any(), any())
    verify(bloodPressureRepository, never()).updateMeasurement(any())

    uiChangeVerification(ui)
  }

  @Suppress("unused")
  fun `params for bp validation errors and expected ui changes`(): List<ValidationErrorsAndUiChangesTestParams> {
    return listOf(
        ValidationErrorsAndUiChangesTestParams("", "80") { ui: Ui -> verify(ui).showSystolicEmptyError() },
        ValidationErrorsAndUiChangesTestParams("120", "") { ui: Ui -> verify(ui).showDiastolicEmptyError() },
        ValidationErrorsAndUiChangesTestParams("999", "80") { ui: Ui -> verify(ui).showSystolicHighError() },
        ValidationErrorsAndUiChangesTestParams("0", "80") { ui: Ui -> verify(ui).showSystolicLowError() },
        ValidationErrorsAndUiChangesTestParams("120", "999") { ui: Ui -> verify(ui).showDiastolicHighError() },
        ValidationErrorsAndUiChangesTestParams("120", "0") { ui: Ui -> verify(ui).showDiastolicLowError() },
        ValidationErrorsAndUiChangesTestParams("120", "121") { ui: Ui -> verify(ui).showSystolicLessThanDiastolicError() }
    )
  }

  data class ValidationErrorsAndUiChangesTestParams(
      val systolic: String,
      val diastolic: String,
      val uiChangeVerification: (Ui) -> Unit
  )

  @Test
  @Parameters(method = "params for OpenAs and bp validation errors")
  fun `when BP entry is active, BP readings are invalid and next arrow is pressed then date entry should not be shown`(
      testParams: ValidationErrorsAndDoNotGoToDateEntryParams
  ) {
    val (openAs, systolic, diastolic) = testParams

    if (openAs is Update) {
      whenever(bloodPressureRepository.measurementImmediate(openAs.bpUuid)).doReturn(TestData.bloodPressureMeasurement(uuid = openAs.bpUuid))
    }

    sheetCreated(openAs)
    uiEvents.run {
      onNext(ScreenChanged(BP_ENTRY))
      onNext(SystolicChanged(systolic))
      onNext(DiastolicChanged(diastolic))
      onNext(ChangeDateClicked)
    }

    verify(ui, never()).showDateEntryScreen()
  }

  @Suppress("unused")
  fun `params for OpenAs and bp validation errors`(): List<ValidationErrorsAndDoNotGoToDateEntryParams> {
    val bpUuid = UUID.fromString("99fed5e5-19a8-4ece-9d07-6beab70ee77c")
    return listOf(
        ValidationErrorsAndDoNotGoToDateEntryParams(New(patientUuid), "", "80"),
        ValidationErrorsAndDoNotGoToDateEntryParams(New(patientUuid), "120", ""),
        ValidationErrorsAndDoNotGoToDateEntryParams(New(patientUuid), "999", "80"),
        ValidationErrorsAndDoNotGoToDateEntryParams(New(patientUuid), "0", "80"),
        ValidationErrorsAndDoNotGoToDateEntryParams(New(patientUuid), "120", "999"),
        ValidationErrorsAndDoNotGoToDateEntryParams(New(patientUuid), "120", "0"),
        ValidationErrorsAndDoNotGoToDateEntryParams(New(patientUuid), "120", "140"),

        ValidationErrorsAndDoNotGoToDateEntryParams(Update(bpUuid), "", "80"),
        ValidationErrorsAndDoNotGoToDateEntryParams(Update(bpUuid), "120", ""),
        ValidationErrorsAndDoNotGoToDateEntryParams(Update(bpUuid), "999", "80"),
        ValidationErrorsAndDoNotGoToDateEntryParams(Update(bpUuid), "0", "80"),
        ValidationErrorsAndDoNotGoToDateEntryParams(Update(bpUuid), "120", "999"),
        ValidationErrorsAndDoNotGoToDateEntryParams(Update(bpUuid), "120", "0"),
        ValidationErrorsAndDoNotGoToDateEntryParams(Update(bpUuid), "120", "140"))
  }

  data class ValidationErrorsAndDoNotGoToDateEntryParams(
      val openAs: OpenAs,
      val systolic: String,
      val diastolic: String
  )

  private fun sheetCreatedForNew(patientUuid: UUID) {
    val openAsNew = New(patientUuid)
    instantiateFixture(openAsNew)
  }

  private fun sheetCreatedForUpdate(existingBpUuid: UUID) {
    val openAsUpdate = Update(existingBpUuid)
    instantiateFixture(openAsUpdate)
  }

  private fun sheetCreated(openAs: OpenAs) {
    when (openAs) {
      is New -> sheetCreatedForNew(openAs.patientUuid)
      is Update -> sheetCreatedForUpdate(openAs.bpUuid)
      else -> throw IllegalStateException("Unknown `openAs`: $openAs")
    }
  }

  private fun instantiateFixture(openAs: OpenAs) {
    val effectHandler = BloodPressureEntryEffectHandler(
        ui = ui,
        patientRepository = patientRepository,
        bloodPressureRepository = bloodPressureRepository,
        appointmentsRepository = appointmentRepository,
        userClock = testUserClock,
        schedulersProvider = TestSchedulersProvider.trampoline(),
        uuidGenerator = FakeUuidGenerator.fixed(UUID.fromString("abb04673-6ec0-4e1a-a4ad-5380e6f7e233")),
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
