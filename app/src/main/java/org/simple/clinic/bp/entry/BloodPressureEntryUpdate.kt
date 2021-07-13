package org.simple.clinic.bp.entry

import com.spotify.mobius.Next
import com.spotify.mobius.Next.noChange
import com.spotify.mobius.Update
import org.simple.clinic.bp.BloodPressureReading
import org.simple.clinic.bp.ValidationResult
import org.simple.clinic.bp.entry.BloodPressureEntrySheet.ScreenType.BP_ENTRY
import org.simple.clinic.bp.entry.BloodPressureEntrySheet.ScreenType.DATE_ENTRY
import org.simple.clinic.bp.entry.BloodPressureSaveState.NOT_SAVING_BLOOD_PRESSURE
import org.simple.clinic.bp.entry.BloodPressureSaveState.SAVING_BLOOD_PRESSURE
import org.simple.clinic.mobius.dispatch
import org.simple.clinic.mobius.next
import org.simple.clinic.util.UserInputDatePaddingCharacter
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator.Result
import java.time.LocalDate

class BloodPressureEntryUpdate(
    private val dateValidator: UserInputDateValidator,
    private val dateInUserTimeZone: LocalDate,
    private val inputDatePaddingCharacter: UserInputDatePaddingCharacter
) : Update<BloodPressureEntryModel, BloodPressureEntryEvent, BloodPressureEntryEffect> {
  override fun update(
      model: BloodPressureEntryModel,
      event: BloodPressureEntryEvent
  ): Next<BloodPressureEntryModel, BloodPressureEntryEffect> {
    return when (event) {
      is ScreenChanged -> next(model.screenChanged(event.type))
      is SystolicChanged -> onSystolicChanged(model, event)
      is DiastolicChanged -> next(model.diastolicChanged(event.diastolic), HideBpErrorMessage)
      is DiastolicBackspaceClicked -> onDiastolicBackSpaceClicked(model)
      is BloodPressureMeasurementFetched -> onBloodPressureMeasurementFetched(model, event)
      is RemoveBloodPressureClicked -> dispatch(ShowConfirmRemoveBloodPressureDialog((model.openAs as OpenAs.Update).bpUuid))
      is BackPressed -> onBackPressed(model)
      is DayChanged -> onDateChanged(model.dayChanged(event.day))
      is MonthChanged -> onDateChanged(model.monthChanged(event.month))
      is YearChanged -> onDateChanged(model.yearChanged(event.fourDigitYear))
      is ChangeDateClicked -> changeDateClicked(model)
      is SaveClicked -> onSaveClicked(model)
      is ShowBpClicked -> showBpClicked(model)
      is BloodPressureSaved -> next(model.bloodPressureStateChanged(NOT_SAVING_BLOOD_PRESSURE), SetBpSavedResultAndFinish)
      is DatePrefilled -> next(model.datePrefilled(event.prefilledDate))
    }
  }

  private fun onSystolicChanged(
      model: BloodPressureEntryModel,
      event: SystolicChanged
  ): Next<BloodPressureEntryModel, BloodPressureEntryEffect> {
    val updatedSystolicModel = model.systolicChanged(event.systolic)
    val effects = if (isSystolicValueComplete(event.systolic)) {
      setOf(HideBpErrorMessage, ChangeFocusToDiastolic)
    } else {
      setOf(HideBpErrorMessage)
    }
    return next(updatedSystolicModel, *effects.toTypedArray())
  }

  private fun onDiastolicBackSpaceClicked(
      model: BloodPressureEntryModel
  ): Next<BloodPressureEntryModel, BloodPressureEntryEffect> {
    return if (model.diastolic.isNotEmpty()) {
      next(model.deleteDiastolicLastDigit())
    } else {
      val deleteSystolicLastDigitModel = model.deleteSystolicLastDigit()
      next(deleteSystolicLastDigitModel, ChangeFocusToSystolic, SetSystolic(deleteSystolicLastDigitModel.systolic))
    }
  }

  private fun onBloodPressureMeasurementFetched(
      model: BloodPressureEntryModel,
      event: BloodPressureMeasurementFetched
  ): Next<BloodPressureEntryModel, BloodPressureEntryEffect> {
    val (systolic, diastolic, recordedAt) = event
    val modelWithSystolicAndDiastolic = model
        .systolicChanged(systolic.toString())
        .diastolicChanged(diastolic.toString())

    return next(
        modelWithSystolicAndDiastolic,
        SetSystolic(systolic.toString()),
        SetDiastolic(diastolic.toString()),
        PrefillDate.forUpdateEntry(recordedAt)
    )
  }

  private fun onBackPressed(
      model: BloodPressureEntryModel
  ): Next<BloodPressureEntryModel, BloodPressureEntryEffect> {
    return when (model.activeScreen) {
      BP_ENTRY -> dispatch(Dismiss as BloodPressureEntryEffect)
      DATE_ENTRY -> showBpClicked(model)
    }
  }

  private fun onDateChanged(
      updatedModel: BloodPressureEntryModel
  ): Next<BloodPressureEntryModel, BloodPressureEntryEffect> =
      next(updatedModel, HideDateErrorMessage)

  private fun changeDateClicked(
      model: BloodPressureEntryModel
  ): Next<BloodPressureEntryModel, BloodPressureEntryEffect> {
    val result = validateEnteredBp(model)
    val effect = if (result is ValidationResult.Valid) {
      ShowDateEntryScreen
    } else {
      ShowBpValidationError(result)
    }
    return dispatch(effect)
  }

  private fun onSaveClicked(
      model: BloodPressureEntryModel
  ): Next<BloodPressureEntryModel, BloodPressureEntryEffect> {
    return if (model.bloodPressureSaveState == SAVING_BLOOD_PRESSURE) {
      noChange()
    } else {
      val bpValidationResult = validateEnteredBp(model)
      val dateValidationResult = dateValidator.validate(getDateText(model), dateInUserTimeZone)
      val validationErrorEffects = getValidationErrorEffects(bpValidationResult, dateValidationResult)

      if (validationErrorEffects.isNotEmpty()) {
        Next.dispatch(validationErrorEffects)
      } else {
        val bpReading = (bpValidationResult as ValidationResult.Valid).reading
        next(model.bloodPressureStateChanged(SAVING_BLOOD_PRESSURE), getCreateOrUpdateEntryEffect(model, dateValidationResult, bpReading))
      }
    }
  }

  private fun validateEnteredBp(model: BloodPressureEntryModel): ValidationResult {
    return when {
      model.systolic.isBlank() -> ValidationResult.ErrorSystolicEmpty
      model.diastolic.isBlank() -> ValidationResult.ErrorDiastolicEmpty
      else -> {
        val bloodPressureReading = BloodPressureReading(systolic = model.systolic.trim().toInt(), diastolic = model.diastolic.trim().toInt())
        bloodPressureReading.validate()
      }
    }
  }

  private fun showBpClicked(
      model: BloodPressureEntryModel
  ): Next<BloodPressureEntryModel, BloodPressureEntryEffect> {
    val result = dateValidator.validate(getDateText(model), dateInUserTimeZone)
    val effect = if (result is Result.Valid) {
      ShowBpEntryScreen(result.parsedDate)
    } else {
      ShowDateValidationError(result)
    }
    return dispatch(effect)
  }

  private fun getCreateOrUpdateEntryEffect(
      model: BloodPressureEntryModel,
      dateValidationResult: Result,
      reading: BloodPressureReading
  ): BloodPressureEntryEffect {
    val userEnteredDate = (dateValidationResult as Result.Valid).parsedDate
    val prefilledDate = model.prefilledDate!!

    return when (val openAs = model.openAs) {
      is OpenAs.New -> CreateNewBpEntry(openAs.patientUuid, reading, userEnteredDate, prefilledDate)
      is OpenAs.Update -> UpdateBpEntry(openAs.bpUuid, reading, userEnteredDate, prefilledDate)
    }
  }

  private fun getValidationErrorEffects(
      bpValidationResult: ValidationResult,
      dateValidationResult: Result
  ): Set<BloodPressureEntryEffect> {
    val validationErrorEffects = mutableSetOf<BloodPressureEntryEffect>()

    if (bpValidationResult !is ValidationResult.Valid) {
      validationErrorEffects.add(ShowBpValidationError(bpValidationResult))
    }

    if (dateValidationResult !is Result.Valid) {
      validationErrorEffects.add(ShowDateValidationError(dateValidationResult))
    }
    return validationErrorEffects.toSet()
  }

  private fun getDateText(model: BloodPressureEntryModel) =
      formatToPaddedDate(model.day, model.month, model.fourDigitYear)

  private fun isSystolicValueComplete(systolicText: String): Boolean {
    return (systolicText.length == 3 && systolicText.matches("^[123].*$".toRegex()))
        || (systolicText.length == 2 && systolicText.matches("^[789].*$".toRegex()))
  }

  private fun formatToPaddedDate(day: String, month: String, fourDigitYear: String): String {
    val paddedDd = day.padStart(length = 2, padChar = inputDatePaddingCharacter.value)
    val paddedMm = month.padStart(length = 2, padChar = inputDatePaddingCharacter.value)

    return "$paddedDd/$paddedMm/$fourDigitYear"
  }
}
