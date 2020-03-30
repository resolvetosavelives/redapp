package org.simple.clinic.summary.bloodpressures

import com.f2prateek.rx.preferences2.Preference
import com.spotify.mobius.rx2.RxMobius
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.ObservableTransformer
import io.reactivex.Scheduler
import org.simple.clinic.bp.BloodPressureRepository
import org.simple.clinic.facility.FacilityRepository
import org.simple.clinic.user.UserSession
import org.simple.clinic.util.scheduler.SchedulersProvider
import javax.inject.Named

class BloodPressureSummaryViewEffectHandler @AssistedInject constructor(
    private val userSession: UserSession,
    private val facilityRepository: FacilityRepository,
    private val bloodPressureRepository: BloodPressureRepository,
    private val schedulersProvider: SchedulersProvider,
    @Assisted private val uiActions: BloodPressureSummaryViewUiActions,
    @Named("is_facility_switched") private val isFacilitySwitchedPreference: Preference<Boolean>
) {

  @AssistedInject.Factory
  interface Factory {
    fun create(uiActions: BloodPressureSummaryViewUiActions): BloodPressureSummaryViewEffectHandler
  }

  fun build(): ObservableTransformer<BloodPressureSummaryViewEffect, BloodPressureSummaryViewEvent> {
    return RxMobius
        .subtypeEffectHandler<BloodPressureSummaryViewEffect, BloodPressureSummaryViewEvent>()
        .addTransformer(LoadBloodPressures::class.java, loadBloodPressureHistory(schedulersProvider.io()))
        .addTransformer(LoadBloodPressuresCount::class.java, loadBloodPressuresCount(schedulersProvider.io()))
        .addTransformer(LoadCurrentFacility::class.java, loadCurrentFacility(schedulersProvider.io()))
        .addTransformer(ShouldShowFacilityChangeAlert::class.java, checkFacilitySwitchFlag(schedulersProvider.io()))
        .addConsumer(OpenBloodPressureEntrySheet::class.java, { uiActions.openBloodPressureEntrySheet(it.patientUuid) }, schedulersProvider.ui())
        .addConsumer(OpenBloodPressureUpdateSheet::class.java, { uiActions.openBloodPressureUpdateSheet(it.measurement.uuid) }, schedulersProvider.ui())
        .addConsumer(ShowBloodPressureHistoryScreen::class.java, { uiActions.showBloodPressureHistoryScreen(it.patientUuid) }, schedulersProvider.ui())
        .addConsumer(OpenAlertFacilityChangeSheet::class.java, { uiActions.openAlertFacilityChangeSheet(it.currentFacility.name) }, schedulersProvider.ui())
        .build()
  }

  private fun checkFacilitySwitchFlag(io: Scheduler): ObservableTransformer<ShouldShowFacilityChangeAlert, BloodPressureSummaryViewEvent> {
    return ObservableTransformer { effect ->
      effect
          .observeOn(io)
          .map { isFacilitySwitchedPreference.get() }
          .map(::ShowFacilityChangeAlert)
    }
  }

  private fun loadBloodPressureHistory(
      scheduler: Scheduler
  ): ObservableTransformer<LoadBloodPressures, BloodPressureSummaryViewEvent> {
    return ObservableTransformer { effect ->
      effect
          .switchMap {
            bloodPressureRepository
                .newestMeasurementsForPatient(it.patientUuid, it.numberOfBpsToDisplay)
                .subscribeOn(scheduler)
          }
          .map(::BloodPressuresLoaded)
    }
  }

  private fun loadBloodPressuresCount(
      scheduler: Scheduler
  ): ObservableTransformer<LoadBloodPressuresCount, BloodPressureSummaryViewEvent> {
    return ObservableTransformer { effect ->
      effect
          .switchMap {
            bloodPressureRepository
                .bloodPressureCount(it.patientUuid)
                .subscribeOn(scheduler)
          }
          .map(::BloodPressuresCountLoaded)
    }
  }

  private fun loadCurrentFacility(scheduler: Scheduler): ObservableTransformer<LoadCurrentFacility, BloodPressureSummaryViewEvent> {
    return ObservableTransformer { effects ->
      effects
          .observeOn(scheduler)
          .flatMap {
            val user = userSession.loggedInUserImmediate()
            requireNotNull(user)

            facilityRepository
                .currentFacility(user)
                .take(1)
          }
          .map(::CurrentFacilityLoaded)
    }
  }
}
