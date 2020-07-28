package org.simple.clinic.forgotpin.createnewpin

import com.spotify.mobius.rx2.RxMobius
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.ObservableTransformer
import org.simple.clinic.facility.FacilityRepository
import org.simple.clinic.user.UserSession
import org.simple.clinic.util.scheduler.SchedulersProvider

class ForgotPinCreateNewEffectHandler @AssistedInject constructor(
    private val userSession: UserSession,
    private val facilityRepository: FacilityRepository,
    private val schedulersProvider: SchedulersProvider,
    @Assisted private val uiActions: UiActions
) {

  @AssistedInject.Factory
  interface Factory {
    fun create(uiActions: UiActions): ForgotPinCreateNewEffectHandler
  }

  fun build(): ObservableTransformer<ForgotPinCreateNewEffect, ForgotPinCreateNewEvent> = RxMobius
      .subtypeEffectHandler<ForgotPinCreateNewEffect, ForgotPinCreateNewEvent>()
      .addTransformer(LoadLoggedInUser::class.java, loadLoggedInUser())
      .addTransformer(LoadCurrentFacility::class.java, loadFacility())
      .build()

  private fun loadFacility(): ObservableTransformer<LoadCurrentFacility, ForgotPinCreateNewEvent> {
    return ObservableTransformer { effects ->
      effects
          .observeOn(schedulersProvider.io())
          .flatMap { userSession.requireLoggedInUser() }
          .switchMap { facilityRepository.currentFacility(it) }
          .map(::CurrentFacilityLoaded)
    }
  }

  private fun loadLoggedInUser(): ObservableTransformer<LoadLoggedInUser, ForgotPinCreateNewEvent> {
    return ObservableTransformer { effects ->
      effects
          .observeOn(schedulersProvider.io())
          .flatMap { userSession.requireLoggedInUser() }
          .map(::LoggedInUserLoaded)
    }
  }
}
