package org.simple.clinic.main

import com.f2prateek.rx.preferences2.Preference
import com.spotify.mobius.rx2.RxMobius
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import org.simple.clinic.user.NewlyVerifiedUser
import org.simple.clinic.user.UserSession
import org.simple.clinic.util.UtcClock
import org.simple.clinic.util.filterTrue
import org.simple.clinic.util.scheduler.SchedulersProvider
import java.time.Instant
import javax.inject.Named

class TheActivityEffectHandler @AssistedInject constructor(
    private val schedulers: SchedulersProvider,
    private val userSession: UserSession,
    private val utcClock: UtcClock,
    @Named("should_lock_after") private val lockAfterTimestamp: Preference<Instant>,
    @Assisted private val uiActions: TheActivityUiActions
) {

  @AssistedInject.Factory
  interface InjectionFactory {
    fun create(uiActions: TheActivityUiActions): TheActivityEffectHandler
  }

  fun build(): ObservableTransformer<TheActivityEffect, TheActivityEvent> {
    return RxMobius
        .subtypeEffectHandler<TheActivityEffect, TheActivityEvent>()
        .addTransformer(LoadAppLockInfo::class.java, loadShowAppLockInto())
        .addAction(ClearLockAfterTimestamp::class.java, { lockAfterTimestamp.delete() }, schedulers.io())
        .addAction(ShowAppLockScreen::class.java, uiActions::showAppLockScreen, schedulers.ui())
        .addTransformer(UpdateLockTimestamp::class.java, updateAppLockTime())
        .addTransformer(ListenForUserVerifications::class.java, listenForUserVerifications())
        .build()
  }

  private fun loadShowAppLockInto(): ObservableTransformer<LoadAppLockInfo, TheActivityEvent> {
    return ObservableTransformer { effects ->
      effects
          .switchMap {
            userSession
                .loggedInUser()
                .subscribeOn(schedulers.io())
                .map {
                  AppLockInfoLoaded(
                      user = it,
                      currentTimestamp = Instant.now(utcClock),
                      lockAtTimestamp = lockAfterTimestamp.get()
                  )
                }
          }
    }
  }

  private fun updateAppLockTime(): ObservableTransformer<UpdateLockTimestamp, TheActivityEvent> {
    return ObservableTransformer { effects ->
      effects
          .observeOn(schedulers.io())
          .switchMap { effect ->
            Observable
                .fromCallable { userSession.isUserLoggedIn() }
                .filterTrue()
                .filter { !lockAfterTimestamp.isSet }
                .doOnNext { lockAfterTimestamp.set(effect.lockAt) }
                .flatMap { Observable.empty<TheActivityEvent>() }
          }
    }
  }

  private fun listenForUserVerifications(): ObservableTransformer<ListenForUserVerifications, TheActivityEvent> {
    return ObservableTransformer { effects ->
      effects
          .switchMap {
            userSession
                .loggedInUser()
                .observeOn(schedulers.io())
                .compose(NewlyVerifiedUser())
                .map { UserWasJustVerified }
          }
    }
  }
}
