package org.simple.clinic.bp.entry

import com.google.firebase.perf.metrics.AddTrace
import com.spotify.mobius.rx2.RxMobius
import dagger.Lazy
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.reactivex.ObservableTransformer
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.cast
import org.simple.clinic.ReportAnalyticsEvents
import org.simple.clinic.bp.BloodPressureMeasurement
import org.simple.clinic.bp.BloodPressureRepository
import org.simple.clinic.bp.ValidationResult
import org.simple.clinic.bp.ValidationResult.ErrorDiastolicEmpty
import org.simple.clinic.bp.ValidationResult.ErrorDiastolicTooHigh
import org.simple.clinic.bp.ValidationResult.ErrorDiastolicTooLow
import org.simple.clinic.bp.ValidationResult.ErrorSystolicEmpty
import org.simple.clinic.bp.ValidationResult.ErrorSystolicLessThanDiastolic
import org.simple.clinic.bp.ValidationResult.ErrorSystolicTooHigh
import org.simple.clinic.bp.ValidationResult.ErrorSystolicTooLow
import org.simple.clinic.bp.entry.PrefillDate.PrefillSpecificDate
import org.simple.clinic.facility.Facility
import org.simple.clinic.overdue.AppointmentRepository
import org.simple.clinic.patient.PatientRepository
import org.simple.clinic.user.User
import org.simple.clinic.util.UserClock
import org.simple.clinic.util.exhaustive
import org.simple.clinic.util.scheduler.SchedulersProvider
import org.simple.clinic.util.toLocalDateAtZone
import org.simple.clinic.util.toUtcInstant
import org.simple.clinic.uuid.UuidGenerator
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator.Result
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator.Result.Invalid.DateIsInFuture
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator.Result.Invalid.InvalidPattern
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class BloodPressureEntryEffectHandler @AssistedInject constructor(
    @Assisted private val ui: BloodPressureEntryUi,
    private val patientRepository: PatientRepository,
    private val bloodPressureRepository: BloodPressureRepository,
    private val appointmentsRepository: AppointmentRepository,
    private val userClock: UserClock,
    private val schedulersProvider: SchedulersProvider,
    private val uuidGenerator: UuidGenerator,
    private val currentUser: Lazy<User>,
    private val currentFacility: Lazy<Facility>
) {

  @AssistedFactory
  interface Factory {
    fun create(ui: BloodPressureEntryUi): BloodPressureEntryEffectHandler
  }

  private val reportAnalyticsEvents = ReportAnalyticsEvents()

  fun build(): ObservableTransformer<BloodPressureEntryEffect, BloodPressureEntryEvent> {
    return RxMobius
        .subtypeEffectHandler<BloodPressureEntryEffect, BloodPressureEntryEvent>()
        .addTransformer(PrefillDate::class.java, prefillDate(schedulersProvider.ui()))
        .addAction(HideBpErrorMessage::class.java, ui::hideBpErrorMessage, schedulersProvider.ui())
        .addAction(ChangeFocusToDiastolic::class.java, ui::changeFocusToDiastolic, schedulersProvider.ui())
        .addAction(ChangeFocusToSystolic::class.java, ui::changeFocusToSystolic, schedulersProvider.ui())
        .addConsumer(SetSystolic::class.java, { ui.setSystolic(it.systolic) }, schedulersProvider.ui())
        .addTransformer(FetchBloodPressureMeasurement::class.java, fetchBloodPressureMeasurement(schedulersProvider.io()))
        .addConsumer(SetDiastolic::class.java, { ui.setDiastolic(it.diastolic) }, schedulersProvider.ui())
        .addConsumer(ShowConfirmRemoveBloodPressureDialog::class.java, { ui.showConfirmRemoveBloodPressureDialog(it.bpUuid) }, schedulersProvider.ui())
        .addAction(Dismiss::class.java, ui::dismiss, schedulersProvider.ui())
        .addAction(HideDateErrorMessage::class.java, ui::hideDateErrorMessage, schedulersProvider.ui())
        .addConsumer(ShowBpValidationError::class.java, { showBpValidationError(it.result) }, schedulersProvider.ui())
        .addAction(ShowDateEntryScreen::class.java, ui::showDateEntryScreen, schedulersProvider.ui())
        .addConsumer(ShowBpEntryScreen::class.java, { showBpEntryScreen(it.date) }, schedulersProvider.ui())
        .addConsumer(ShowDateValidationError::class.java, { showDateValidationError(it.result) }, schedulersProvider.ui())
        .addTransformer(CreateNewBpEntry::class.java, createNewBpEntryTransformer())
        .addAction(SetBpSavedResultAndFinish::class.java, ui::setBpSavedResultAndFinish, schedulersProvider.ui())
        .addTransformer(UpdateBpEntry::class.java, updateBpEntryTransformer())
        .build()
  }

  private fun prefillDate(scheduler: Scheduler): ObservableTransformer<PrefillDate, BloodPressureEntryEvent> {
    return ObservableTransformer { prefillDates ->
      prefillDates
          .map(::convertToLocalDate)
          .observeOn(scheduler)
          .doOnNext(ui::setDateOnInputFields)
          .doOnNext(ui::showBpDate)
          .map { DatePrefilled(it) }
    }
  }

  private fun convertToLocalDate(prefillDate: PrefillDate): LocalDate {
    val instant = if (prefillDate is PrefillSpecificDate) prefillDate.date else Instant.now(userClock)
    return instant.toLocalDateAtZone(userClock.zone)
  }

  private fun fetchBloodPressureMeasurement(
      scheduler: Scheduler
  ): ObservableTransformer<FetchBloodPressureMeasurement, BloodPressureEntryEvent> {
    return ObservableTransformer { bloodPressureMeasurements ->
      bloodPressureMeasurements
          .observeOn(scheduler)
          .map { getExistingBloodPressureMeasurement(it.bpUuid) }
          .map { BloodPressureMeasurementFetched(it.reading.systolic, it.reading.diastolic, it.recordedAt) }
    }
  }

  private fun showBpValidationError(bpValidation: ValidationResult) {
    when (bpValidation) {
      is ErrorSystolicLessThanDiastolic -> ui.showSystolicLessThanDiastolicError()
      is ErrorSystolicTooHigh -> ui.showSystolicHighError()
      is ErrorSystolicTooLow -> ui.showSystolicLowError()
      is ErrorDiastolicTooHigh -> ui.showDiastolicHighError()
      is ErrorDiastolicTooLow -> ui.showDiastolicLowError()
      is ErrorSystolicEmpty -> ui.showSystolicEmptyError()
      is ErrorDiastolicEmpty -> ui.showDiastolicEmptyError()
      is ValidationResult.Valid -> {
        /* Nothing to do here. */
      }
    }.exhaustive()
  }

  private fun showBpEntryScreen(entryDate: LocalDate) {
    with(ui) {
      showBpEntryScreen()
      showBpDate(entryDate)
    }
  }

  private fun showDateValidationError(result: Result) {
    when (result) {
      is InvalidPattern -> ui.showInvalidDateError()
      is DateIsInFuture -> ui.showDateIsInFutureError()
      is Result.Valid -> throw IllegalStateException("Date validation error cannot be $result")
    }.exhaustive()
  }

  private fun createNewBpEntryTransformer(): ObservableTransformer<CreateNewBpEntry, BloodPressureEntryEvent> {
    return ObservableTransformer { createNewBpEntries ->
      createNewBpEntries
          .observeOn(schedulersProvider.io())
          .map { createNewBpEntry ->
            val user = currentUser.get()
            val facility = currentFacility.get()

            createNewBloodPressureMeasurement(user, facility, createNewBpEntry)

            BloodPressureSaved(createNewBpEntry.wasDateChanged)
          }
          .compose(reportAnalyticsEvents)
          .cast()
    }
  }

  private fun createNewBloodPressureMeasurement(
      user: User,
      facility: Facility,
      createNewBpEntry: CreateNewBpEntry
  ) {
    val createdBloodPressureMeasurement = storeNewBloodPressureMeasurement(user, facility, createNewBpEntry)

    val entryDate = createNewBpEntry.userEnteredDate.toUtcInstant(userClock)
    markOlderAppointmentsAsVisited(createdBloodPressureMeasurement)
    updatePatientRecordedAtDate(createdBloodPressureMeasurement, entryDate)
  }

  @AddTrace(name = "bpEntry_markAppointmentsVisited")
  private fun markOlderAppointmentsAsVisited(bloodPressureMeasurement: BloodPressureMeasurement) {
    appointmentsRepository.markAppointmentsCreatedBeforeTodayAsVisited(bloodPressureMeasurement.patientUuid)
  }

  private fun updateBpEntryTransformer(): ObservableTransformer<UpdateBpEntry, BloodPressureEntryEvent> {
    return ObservableTransformer { updateBpEntries ->
      updateBpEntries
          .observeOn(schedulersProvider.io())
          .map { updateBpEntry ->
            val updatedBp = getUpdatedBloodPressureMeasurement(updateBpEntry)

            updatedBp to updateBpEntry.wasDateChanged
          }
          .doOnNext { (bloodPressureMeasurement, _) -> storeUpdateBloodPressureMeasurement(bloodPressureMeasurement) }
          .doOnNext { (bloodPressureMeasurement, _) -> updatePatientRecordedAtDate(bloodPressureMeasurement, bloodPressureMeasurement.recordedAt) }
          .map { (_, wasDateChanged) -> BloodPressureSaved(wasDateChanged) }
          .compose(reportAnalyticsEvents)
          .cast()
    }
  }

  private fun getUpdatedBloodPressureMeasurement(
      updateBpEntry: UpdateBpEntry
  ): BloodPressureMeasurement {
    val storedBloodPressureMeasurement = getExistingBloodPressureMeasurement(updateBpEntry.bpUuid)

    val user = currentUser.get()
    val facility = currentFacility.get()

    return updateBloodPressureMeasurementValues(
        existingMeasurement = storedBloodPressureMeasurement,
        userUuid = user.uuid,
        facilityUuid = facility.uuid,
        updateBpEntry = updateBpEntry
    )

  }

  @AddTrace(name = "bpEntry_storeUpdatedBp")
  private fun storeUpdateBloodPressureMeasurement(
      bloodPressureMeasurement: BloodPressureMeasurement
  ) {
    bloodPressureRepository.updateMeasurement(bloodPressureMeasurement)
  }

  @AddTrace(name = "bpEntry_updatePatientRecordedAt")
  private fun updatePatientRecordedAtDate(
      bloodPressureMeasurement: BloodPressureMeasurement,
      entryDate: Instant
  ) {
    patientRepository.compareAndUpdateRecordedAt(bloodPressureMeasurement.patientUuid, entryDate)
  }

  private fun updateBloodPressureMeasurementValues(
      existingMeasurement: BloodPressureMeasurement,
      userUuid: UUID,
      facilityUuid: UUID,
      updateBpEntry: UpdateBpEntry
  ): BloodPressureMeasurement {
    val (_, reading, parsedDateFromForm, _) = updateBpEntry

    return existingMeasurement.copy(
        userUuid = userUuid,
        facilityUuid = facilityUuid,
        reading = reading,
        recordedAt = parsedDateFromForm.toUtcInstant(userClock)
    )
  }

  @AddTrace(name = "bpEntry_storeNewBp")
  private fun storeNewBloodPressureMeasurement(
      user: User,
      currentFacility: Facility,
      entry: CreateNewBpEntry
  ): BloodPressureMeasurement {
    val (patientUuid, reading, date, _) = entry
    return bloodPressureRepository.saveMeasurement(
        patientUuid = patientUuid,
        reading = reading,
        loggedInUser = user,
        currentFacility = currentFacility,
        recordedAt = date.toUtcInstant(userClock),
        uuid = uuidGenerator.v4())
  }

  @AddTrace(name = "bpEntry_getExistingBp")
  private fun getExistingBloodPressureMeasurement(bpUuid: UUID) = bloodPressureRepository.measurementImmediate(bpUuid)
}
