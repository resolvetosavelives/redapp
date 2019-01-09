package org.simple.clinic.widgets

import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding2.support.v7.widget.RecyclerViewScrollEvent
import io.reactivex.ObservableTransformer
import kotlin.math.absoluteValue

enum class RecyclerViewScrollState {
  IDLE,
  DRAGGING,
  SETTLING;

  companion object {
    fun fromIntDef(stateInt: Int): RecyclerViewScrollState {
      return when (stateInt) {
        androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE -> IDLE
        androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING -> DRAGGING
        androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING -> SETTLING
        else -> throw AssertionError("Unknown scroll state: $stateInt")
      }
    }
  }
}

object RecyclerViewUserScrollDetector {

  fun streamDetections(): ObservableTransformer<Pair<RecyclerViewScrollEvent, Int>, RecyclerViewScrolled> {
    return ObservableTransformer { upstream ->
      upstream
          .map { (event, stateInt) -> event to RecyclerViewScrollState.fromIntDef(stateInt) }
          .map { (event, state) ->
            // dY will be 0 if the list cannot be scrolled further in the direction of scroll.
            val byUser = event.dy().absoluteValue > 0 && state == RecyclerViewScrollState.DRAGGING
            RecyclerViewScrolled(byUser)
          }
    }
  }
}

data class RecyclerViewScrolled(val byUser: Boolean)
