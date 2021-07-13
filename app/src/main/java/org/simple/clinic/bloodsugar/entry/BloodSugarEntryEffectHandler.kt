package org.simple.clinic.bloodsugar.entry

import com.f2prateek.rx.preferences2.Preference
import com.spotify.mobius.rx2.RxMobius
import dagger.Lazy
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.reactivex.ObservableTransformer
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.rxkotlin.cast
import org.simple.clinic.ReportAnalyticsEvents
import org.simple.clinic.bloodsugar.BloodSugarMeasurement
import org.simple.clinic.bloodsugar.BloodSugarRepository
import org.simple.clinic.bloodsugar.BloodSugarUnitPreference
import org.simple.clinic.bloodsugar.entry.PrefillDate.PrefillSpecificDate
import org.simple.clinic.bloodsugar.entry.ValidationResult.ErrorBloodSugarEmpty
import org.simple.clinic.bloodsugar.entry.ValidationResult.ErrorBloodSugarTooHigh
import org.simple.clinic.bloodsugar.entry.ValidationResult.ErrorBloodSugarTooLow
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
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator.Result.Invalid.DateIsInFuture
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator.Result.Invalid.InvalidPattern
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator.Result.Valid
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class BloodSugarEntryEffectHandler @AssistedInject constructor(
    @Assisted private val ui: BloodSugarEntryUi,
    private val bloodSugarRepository: BloodSugarRepository,
    private val patientRepository: PatientRepository,
    private val appointmentsRepository: AppointmentRepository,
    private val userClock: UserClock,
    private val schedulersProvider: SchedulersProvider,
    private val currentUser: Lazy<User>,
    private val currentFacility: Lazy<Facility>,
    private val uuidGenerator: UuidGenerator,
    private val bloodSugarUnitPreference: Preference<BloodSugarUnitPreference>
) {
  @AssistedFactory
  interface Factory {
    fun create(ui: BloodSugarEntryUi): BloodSugarEntryEffectHandler
  }

  private val reportAnalyticsEvents = ReportAnalyticsEvents()

  fun build(): ObservableTransformer<BloodSugarEntryEffect, BloodSugarEntryEvent> {
    return RxMobius
        .subtypeEffectHandler<BloodSugarEntryEffect, BloodSugarEntryEvent>()
        .addAction(HideBloodSugarErrorMessage::class.java, ui::hideBloodSugarErrorMessage, schedulersProvider.ui())
        .addAction(HideDateErrorMessage::class.java, ui::hideDateErrorMessage, schedulersProvider.ui())
        .addAction(Dismiss::class.java, ui::dismiss, schedulersProvider.ui())
        .addAction(ShowDateEntryScreen::class.java, ui::showDateEntryScreen, schedulersProvider.ui())
        .addConsumer(ShowBloodSugarValidationError::class.java, { showBloodSugarValidationError(it.result, it.unitPreference) }, schedulersProvider.ui())
        .addConsumer(ShowBloodSugarEntryScreen::class.java, { showBloodSugarEntryScreen(it.date) }, schedulersProvider.ui())
        .addTransformer(PrefillDate::class.java, prefillDate(schedulersProvider.ui()))
        .addConsumer(ShowDateValidationError::class.java, { showDateValidationError(it.result) }, schedulersProvider.ui())
        .addAction(SetBloodSugarSavedResultAndFinish::class.java, ui::setBloodSugarSavedResultAndFinish, schedulersProvider.ui())
        .addTransformer(CreateNewBloodSugarEntry::class.java, createNewBloodSugarEntryTransformer())
        .addTransformer(FetchBloodSugarMeasurement::class.java, fetchBloodSugarMeasurement(schedulersProvider.io()))
        .addConsumer(SetBloodSugarReading::class.java, { ui.setBloodSugarReading(it.bloodSugarReading) }, schedulersProvider.ui())
        .addTransformer(UpdateBloodSugarEntry::class.java, updateBloodSugarEntryTransformer(schedulersProvider.io()))
        .addConsumer(ShowConfirmRemoveBloodSugarDialog::class.java, { ui.showConfirmRemoveBloodSugarDialog(it.bloodSugarMeasurementUuid) }, schedulersProvider.ui())
        .addTransformer(LoadBloodSugarUnitPreference::class.java, loadBloodSugarUnitPreference())
        .addConsumer(ShowBloodSugarUnitSelectionDialog::class.java, { ui.showBloodSugarUnitSelectionDialog(it.bloodSugarUnitPreference) }, schedulersProvider.ui())
        .build()
  }

  private fun loadBloodSugarUnitPreference(): ObservableTransformer<LoadBloodSugarUnitPreference, BloodSugarEntryEvent> {
    return ObservableTransformer { effect ->
      effect
          .observeOn(schedulersProvider.io())
          .switchMap { bloodSugarUnitPreference.asObservable() }
          .map(::BloodSugarUnitPreferenceLoaded)
    }
  }

  private fun fetchBloodSugarMeasurement(
      scheduler: Scheduler
  ): ObservableTransformer<FetchBloodSugarMeasurement, BloodSugarEntryEvent> {
    return ObservableTransformer { fetchBloodSugarMeasurementEffectStream ->
      fetchBloodSugarMeasurementEffectStream
          .observeOn(scheduler)
          .map { getExistingBloodSugarMeasurement(it.bloodSugarMeasurementUuid) }
          .map { BloodSugarMeasurementFetched(it) }
    }
  }

  private fun getExistingBloodSugarMeasurement(bloodSugarMeasurementUuid: UUID): BloodSugarMeasurement? =
      bloodSugarRepository.measurement(bloodSugarMeasurementUuid)

  private fun showBloodSugarValidationError(
      result: ValidationResult,
      unitPreference: BloodSugarUnitPreference
  ) {
    when (result) {
      ErrorBloodSugarEmpty -> ui.showBloodSugarEmptyError()
      is ErrorBloodSugarTooHigh -> ui.showBloodSugarHighError(result.measurementType, unitPreference)
      is ErrorBloodSugarTooLow -> ui.showBloodSugarLowError(result.measurementType, unitPreference)
    }
  }

  private fun showBloodSugarEntryScreen(date: LocalDate) {
    with(ui) {
      showBloodSugarEntryScreen()
      showBloodSugarDate(date)
    }
  }

  private fun prefillDate(scheduler: Scheduler): ObservableTransformer<PrefillDate, BloodSugarEntryEvent> {
    return ObservableTransformer { prefillDates ->
      prefillDates
          .map(::convertToLocalDate)
          .observeOn(scheduler)
          .doOnNext(ui::setDateOnInputFields)
          .doOnNext(ui::showBloodSugarDate)
          .map { DatePrefilled(it) }
    }
  }

  private fun convertToLocalDate(prefillDate: PrefillDate): LocalDate {
    val instant = if (prefillDate is PrefillSpecificDate) prefillDate.date else Instant.now(userClock)
    return instant.toLocalDateAtZone(userClock.zone)
  }

  private fun showDateValidationError(result: UserInputDateValidator.Result) {
    when (result) {
      InvalidPattern -> ui.showInvalidDateError()
      DateIsInFuture -> ui.showDateIsInFutureError()
      is Valid -> throw IllegalStateException("Date validation error cannot be $result")
    }.exhaustive()
  }

  private fun createNewBloodSugarEntryTransformer(): ObservableTransformer<CreateNewBloodSugarEntry, BloodSugarEntryEvent> {
    return ObservableTransformer { createNewBloodSugarEntries ->
      createNewBloodSugarEntries
          .observeOn(schedulersProvider.io())
          .flatMapSingle { createNewBloodSugarEntry ->
            val user = currentUser.get()
            val facility = currentFacility.get()
            storeNewBloodSugarMeasurement(user, facility, createNewBloodSugarEntry)
                .flatMap { updateAppointmentsAsVisited(createNewBloodSugarEntry, it) }
          }
          .compose(reportAnalyticsEvents)
          .cast()
    }
  }

  private fun storeNewBloodSugarMeasurement(
      user: User,
      currentFacility: Facility,
      entry: CreateNewBloodSugarEntry
  ): Single<BloodSugarMeasurement> {
    val (patientUuid, date, _, reading) = entry
    return bloodSugarRepository.saveMeasurement(
        uuid = uuidGenerator.v4(),
        reading = reading,
        patientUuid = patientUuid,
        loggedInUser = user,
        facility = currentFacility,
        recordedAt = date.toUtcInstant(userClock)
    )
  }

  private fun updateAppointmentsAsVisited(
      createNewBloodSugarEntry: CreateNewBloodSugarEntry,
      bloodSugarMeasurement: BloodSugarMeasurement
  ): Single<BloodSugarSaved> {
    val entryDate = createNewBloodSugarEntry.userEnteredDate.toUtcInstant(userClock)

    appointmentsRepository.markAppointmentsCreatedBeforeTodayAsVisited(bloodSugarMeasurement.patientUuid)
    patientRepository.compareAndUpdateRecordedAt(bloodSugarMeasurement.patientUuid, entryDate)

    return Single.just(BloodSugarSaved(createNewBloodSugarEntry.wasDateChanged))
  }

  private fun updateBloodSugarEntryTransformer(scheduler: Scheduler): ObservableTransformer<UpdateBloodSugarEntry, BloodSugarEntryEvent> {
    return ObservableTransformer { updateBloodSugarEntries ->
      updateBloodSugarEntries
          .observeOn(scheduler)
          .map { updateBloodSugarEntry ->
            val updatedBloodSugarMeasurement = updateBloodSugarMeasurement(updateBloodSugarEntry)
            storeUpdateBloodSugarMeasurement(updatedBloodSugarMeasurement)
            BloodSugarSaved(updateBloodSugarEntry.wasDateChanged)
          }
          .compose(reportAnalyticsEvents)
          .cast<BloodSugarEntryEvent>()
    }
  }

  private fun updateBloodSugarMeasurement(updateBloodSugarEntry: UpdateBloodSugarEntry): BloodSugarMeasurement {
    val (_, userEnteredDate, _, bloodSugarReading) = updateBloodSugarEntry
    val bloodSugarMeasurement = getExistingBloodSugarMeasurement(updateBloodSugarEntry.bloodSugarMeasurementUuid)!!
    val user = currentUser.get()
    val facility = currentFacility.get()

    return bloodSugarMeasurement.copy(
        userUuid = user.uuid,
        facilityUuid = facility!!.uuid,
        reading = bloodSugarMeasurement.reading.copy(value = bloodSugarReading.value),
        recordedAt = userEnteredDate.toUtcInstant(userClock)
    )
  }

  private fun storeUpdateBloodSugarMeasurement(bloodSugarMeasurement: BloodSugarMeasurement) {
    bloodSugarRepository.updateMeasurement(bloodSugarMeasurement)
    patientRepository.compareAndUpdateRecordedAt(bloodSugarMeasurement.patientUuid, bloodSugarMeasurement.recordedAt)
  }
}
