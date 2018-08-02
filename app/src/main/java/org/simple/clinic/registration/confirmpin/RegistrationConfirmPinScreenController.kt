package org.simple.clinic.registration.confirmpin

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.withLatestFrom
import org.simple.clinic.registration.RegistrationScheduler
import org.simple.clinic.user.UserSession
import org.simple.clinic.widgets.UiEvent
import org.threeten.bp.Instant
import javax.inject.Inject

typealias Ui = RegistrationConfirmPinScreen
typealias UiChange = (Ui) -> Unit

class RegistrationConfirmPinScreenController @Inject constructor(
    private val userSession: UserSession,
    private val registrationScheduler: RegistrationScheduler
) : ObservableTransformer<UiEvent, UiChange> {

  override fun apply(events: Observable<UiEvent>): ObservableSource<UiChange> {
    val replayedEvents = events.replay().refCount()

    return Observable.merge(
        preFillExistingDetails(replayedEvents),
        enableNextButton(replayedEvents),
        disableNextButton(replayedEvents),
        updateOngoingEntryAndProceed(replayedEvents))
  }

  private fun preFillExistingDetails(events: Observable<UiEvent>): Observable<UiChange> {
    return events
        .ofType<RegistrationConfirmPinScreenCreated>()
        .flatMapSingle {
          userSession.ongoingRegistrationEntry()
              .map { { ui: Ui -> ui.preFillUserDetails(it) } }
        }
  }

  private fun updateOngoingEntryAndProceed(events: Observable<UiEvent>): Observable<UiChange> {
    val pinTextChanges = events.ofType<RegistrationConfirmPinTextChanged>()
    val nextClicks = events.ofType<RegistrationConfirmPinNextClicked>()

    return nextClicks
        .withLatestFrom(pinTextChanges.map { it.confirmPin })
        .flatMap { (_, pinConfirmation) ->
          if (pinConfirmation.length > 4) {
            throw AssertionError("Shouldn't happen")
          }

          userSession.ongoingRegistrationEntry()
              .map { it.copy(pinConfirmation = pinConfirmation, createdAt = Instant.now()) }
              .flatMapCompletable { userSession.saveOngoingRegistrationEntry(it) }
              .andThen(userSession.loginFromOngoingRegistrationEntry())
              .andThen(registrationScheduler.schedule())
              .andThen(Observable.just({ ui: Ui -> ui.openFacilitySelectionScreen() }))
        }
  }

  private fun enableNextButton(events: Observable<UiEvent>): Observable<UiChange> {
    return setNextButtonEnabled(events, true)
  }

  private fun disableNextButton(events: Observable<UiEvent>): Observable<UiChange> {
    return setNextButtonEnabled(events, false)
  }

  private fun setNextButtonEnabled(events: Observable<UiEvent>, enabled: Boolean): Observable<UiChange> {
    return events
        .ofType<RegistrationConfirmPinTextChanged>()
        .map { it.confirmPin.isBlank() }
        .distinctUntilChanged()
        .filter { isBlank -> isBlank != enabled }
        .map { { ui: Ui -> ui.setNextButtonEnabled(enabled) } }
  }
}
