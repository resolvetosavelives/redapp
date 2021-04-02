package org.simple.clinic.datepicker

import org.simple.clinic.appconfig.Country
import org.simple.clinic.datepicker.calendar.CalendarDatePicker
import org.simple.clinic.navigation.v2.ScreenKey
import java.time.LocalDate
import javax.inject.Inject

class DatePickerKeyFactory @Inject constructor(
    private val country: Country
) {

  // TODO: Return appropriate date picker depending on country
  fun key(
      preselectedDate: LocalDate,
      allowedDateRange: ClosedRange<LocalDate>
  ): ScreenKey {
    return CalendarDatePicker.Key(preselectedDate = preselectedDate,
        minDate = allowedDateRange.start,
        maxDate = allowedDateRange.endInclusive)
  }
}
