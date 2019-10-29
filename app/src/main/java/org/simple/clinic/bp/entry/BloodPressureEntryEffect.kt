package org.simple.clinic.bp.entry

import org.simple.clinic.bp.entry.BpValidator.Validation
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator.Result
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import java.util.UUID

sealed class BloodPressureEntryEffect

sealed class PrefillDate : BloodPressureEntryEffect() {
  companion object {
    fun forNewEntry(): PrefillDate {
      return PrefillCurrentDate
    }

    fun forUpdateEntry(date: Instant): PrefillDate {
      return PrefillSpecificDate(date)
    }
  }

  object PrefillCurrentDate : PrefillDate()

  data class PrefillSpecificDate(val date: Instant) : PrefillDate()
}

object HideBpErrorMessage : BloodPressureEntryEffect()

object ChangeFocusToDiastolic : BloodPressureEntryEffect()

object ChangeFocusToSystolic : BloodPressureEntryEffect()

data class SetSystolic(val systolic: String) : BloodPressureEntryEffect()

data class FetchBloodPressureMeasurement(val bpUuid: UUID) : BloodPressureEntryEffect()

data class SetDiastolic(val diastolic: String) : BloodPressureEntryEffect()

data class ShowConfirmRemoveBloodPressureDialog(val bpUuid: UUID) : BloodPressureEntryEffect()

object Dismiss : BloodPressureEntryEffect()

object HideDateErrorMessage : BloodPressureEntryEffect()

data class ShowBpValidationError(val result: Validation) : BloodPressureEntryEffect()

object ShowDateEntryScreen : BloodPressureEntryEffect()

data class ShowBpEntryScreen(val date: LocalDate) : BloodPressureEntryEffect()

data class ShowDateValidationError(val result: Result) : BloodPressureEntryEffect()

data class CreateNewBpEntry(
    val patientUuid: UUID,
    val systolic: Int,
    val diastolic: Int,
    val parsedDateFromForm: LocalDate,
    val prefilledDate: LocalDate
) : BloodPressureEntryEffect() {
  val wasDateChanged: Boolean
    get() = parsedDateFromForm != prefilledDate
}

object SetBpSavedResultAndFinish : BloodPressureEntryEffect()

data class UpdateBpEntry(
    val bpUuid: UUID,
    val systolic: Int,
    val diastolic: Int,
    val parsedDateFromForm: LocalDate,
    val prefilledDate: LocalDate
) : BloodPressureEntryEffect() {
  val wasDateChanged: Boolean
    get() = parsedDateFromForm != prefilledDate
}
