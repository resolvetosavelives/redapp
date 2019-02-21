package org.simple.clinic.sync.indicator

import com.f2prateek.rx.preferences2.Preference
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.ofType
import org.simple.clinic.ReplayUntilScreenIsDestroyed
import org.simple.clinic.ReportAnalyticsEvents
import org.simple.clinic.sync.LastSyncedState
import org.simple.clinic.sync.SyncInterval
import org.simple.clinic.sync.SyncProgress.FAILURE
import org.simple.clinic.sync.SyncProgress.SUCCESS
import org.simple.clinic.sync.SyncProgress.SYNCING
import org.simple.clinic.sync.indicator.SyncIndicatorState.ConnectToSync
import org.simple.clinic.sync.indicator.SyncIndicatorState.SyncPending
import org.simple.clinic.sync.indicator.SyncIndicatorState.Synced
import org.simple.clinic.sync.indicator.SyncIndicatorState.Syncing
import org.simple.clinic.util.UtcClock
import org.simple.clinic.widgets.UiEvent
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import javax.inject.Inject

typealias Ui = SyncIndicatorView
typealias UiChange = (Ui) -> Unit

class SyncIndicatorViewController @Inject constructor(
    private val lastSyncState: Preference<LastSyncedState>,
    private val utcClock: UtcClock
) : ObservableTransformer<UiEvent, UiChange> {

  override fun apply(events: Observable<UiEvent>): ObservableSource<UiChange> {
    val replayedEvents = ReplayUntilScreenIsDestroyed(events)
        .compose(ReportAnalyticsEvents())
        .replay()

    return updateIndicatorView(replayedEvents)
  }

  private fun updateIndicatorView(events: Observable<UiEvent>): Observable<UiChange> {
    val screenCreated = events.ofType<SyncIndicatorViewCreated>()
    val lastSyncedStateStream = lastSyncState
        .asObservable()
        .distinctUntilChanged()

    val showDefaultSyncIndicatorState = lastSyncedStateStream
        .filter { it.lastSyncProgress == null }
        .map { { ui: Ui -> ui.updateState(SyncPending) } }

    val syncProgress = lastSyncedStateStream.filter { it.lastSyncProgress != null }

    val showSyncIndicatorState = Observables
        .combineLatest(screenCreated, syncProgress)
        { _, stateStream ->
          val indicatorState = when (stateStream.lastSyncProgress!!) {
            SUCCESS -> syncIndicatorState(stateStream.lastSyncSuccessTimestamp)
            FAILURE -> syncedFailureState(stateStream.lastSyncSuccessTimestamp)
            SYNCING -> Syncing
          }
          { ui: Ui -> ui.updateState(indicatorState) }
        }

    return showSyncIndicatorState.mergeWith(showDefaultSyncIndicatorState)
  }

  private fun syncedFailureState(timestamp: Instant?): SyncIndicatorState {
    if (timestamp == null) {
      return SyncPending
    }

    return syncIndicatorState(timestamp)
  }

  private fun syncIndicatorState(timestamp: Instant?): SyncIndicatorState {
    val now = Instant.now(utcClock)
    val timeSinceLastSync = Duration.between(timestamp, now)

    val maxIntervalSinceLastSync = Duration.ofHours(12)
    val mostFrequentSyncInterval = enumValues<SyncInterval>()
        .map { it.frequency }
        .min()!!

    return when {
      timeSinceLastSync > maxIntervalSinceLastSync -> ConnectToSync
      timeSinceLastSync > mostFrequentSyncInterval -> SyncPending
      timeSinceLastSync.isNegative -> SyncPending
      else -> Synced(timeSinceLastSync.toMinutes())
    }
  }
}
