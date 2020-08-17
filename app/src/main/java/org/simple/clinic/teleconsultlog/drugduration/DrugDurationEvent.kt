package org.simple.clinic.teleconsultlog.drugduration

import org.simple.clinic.widgets.UiEvent

sealed class DrugDurationEvent : UiEvent

object DurationChanged : DrugDurationEvent() {
  override val analyticsName: String = "Drug Duration:Duration Changed"
}
