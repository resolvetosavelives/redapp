package org.simple.clinic.bp.entry

import org.simple.clinic.bp.entry.BloodPressureEntrySheet.ScreenType
import org.simple.clinic.widgets.UiEvent
import java.time.Instant
import java.time.LocalDate

sealed class BloodPressureEntryEvent : UiEvent

data class SystolicChanged(val systolic: String) : BloodPressureEntryEvent() {
  override val analyticsName = "Blood Pressure Entry:Systolic Text Changed"
}

data class DiastolicChanged(val diastolic: String) : BloodPressureEntryEvent() {
  override val analyticsName = "Blood Pressure Entry:Diastolic Text Changed"
}

object SaveClicked : BloodPressureEntryEvent() {
  override val analyticsName = "Blood Pressure Entry:Save Clicked"
}

object RemoveBloodPressureClicked : BloodPressureEntryEvent() {
  override val analyticsName = "Blood Pressure Entry:Remove Clicked"
}

object DiastolicBackspaceClicked : BloodPressureEntryEvent()

data class ScreenChanged(val type: ScreenType) : BloodPressureEntryEvent()

data class DayChanged(val day: String) : BloodPressureEntryEvent()

data class MonthChanged(val month: String) : BloodPressureEntryEvent()

data class YearChanged(val fourDigitYear: String) : BloodPressureEntryEvent()

data class BloodPressureSaved(val wasDateChanged: Boolean) : BloodPressureEntryEvent() {
  override val analyticsName = when {
    wasDateChanged -> "Blood Pressure Entry:BP Saved With Current Date"
    else -> "Blood Pressure Entry:BP Saved With An Older Date"
  }
}

object ChangeDateClicked : BloodPressureEntryEvent() {
  override val analyticsName = "Blood Pressure Entry:Next Arrow Clicked"
}

object ShowBpClicked : BloodPressureEntryEvent() {
  override val analyticsName = "Blood Pressure Entry:Previous Arrow Clicked"
}

object BackPressed : BloodPressureEntryEvent() {
  override val analyticsName = "Blood Pressure Entry:Hardware Back Pressed"
}

data class BloodPressureMeasurementFetched(
    val systolic: Int,
    val diastolic: Int,
    val recordedAt: Instant
) : BloodPressureEntryEvent()

data class DatePrefilled(val prefilledDate: LocalDate) : BloodPressureEntryEvent()
