package org.simple.clinic.bloodsugar.entry

import org.simple.clinic.bloodsugar.BloodSugarMeasurement
import org.simple.clinic.bloodsugar.BloodSugarUnitPreference
import org.simple.clinic.bloodsugar.entry.BloodSugarEntrySheet.ScreenType
import org.simple.clinic.widgets.UiEvent
import java.time.LocalDate

sealed class BloodSugarEntryEvent : UiEvent

data class BloodSugarChanged(
    val bloodSugarReading: String
) : BloodSugarEntryEvent() {
  override val analyticsName: String = "Blood Sugar Entry:Blood Sugar Text Changed"
}

data class DayChanged(
    val day: String
) : BloodSugarEntryEvent()

data class MonthChanged(
    val month: String
) : BloodSugarEntryEvent()

data class YearChanged(
    val fourDigitYear: String
) : BloodSugarEntryEvent()

data class ScreenChanged(val type: ScreenType) : BloodSugarEntryEvent()

data class BloodSugarSaved(val wasDateChanged: Boolean) : BloodSugarEntryEvent() {
  override val analyticsName: String = when {
    wasDateChanged -> "Blood Sugar Entry:Blood Sugar Saved With Current Date"
    else -> "Blood Sugar Entry:Blood Sugar With An Older Date"
  }
}

data class DatePrefilled(val prefilledDate: LocalDate) : BloodSugarEntryEvent()

object BackPressed : BloodSugarEntryEvent()

object ChangeDateClicked : BloodSugarEntryEvent()

object ShowBloodSugarEntryClicked : BloodSugarEntryEvent()

object SaveClicked : BloodSugarEntryEvent() {
  override val analyticsName: String = "Blood Sugar Entry:Save Clicked"
}

data class BloodSugarMeasurementFetched(val bloodSugarMeasurement: BloodSugarMeasurement) : BloodSugarEntryEvent()

object RemoveBloodSugarClicked : BloodSugarEntryEvent() {
  override val analyticsName: String = "Blood Sugar Entry: Remove clicked"
}

data class BloodSugarUnitPreferenceLoaded(val bloodSugarUnitPreference: BloodSugarUnitPreference) : BloodSugarEntryEvent()

object BloodSugarReadingUnitButtonClicked : BloodSugarEntryEvent()
