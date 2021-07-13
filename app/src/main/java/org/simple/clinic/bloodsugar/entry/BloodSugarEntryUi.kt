package org.simple.clinic.bloodsugar.entry

import org.simple.clinic.bloodsugar.BloodSugarMeasurementType
import org.simple.clinic.bloodsugar.BloodSugarUnitPreference
import java.time.LocalDate
import java.util.UUID

interface BloodSugarEntryUi {
  fun setBloodSugarSavedResultAndFinish()
  fun hideBloodSugarErrorMessage()
  fun showBloodSugarEmptyError()
  fun showBloodSugarHighError(
      measurementType: BloodSugarMeasurementType,
      unitPreference: BloodSugarUnitPreference
  )

  fun showBloodSugarLowError(
      measurementType: BloodSugarMeasurementType,
      unitPreference: BloodSugarUnitPreference
  )

  fun showBloodSugarEntryScreen()
  fun showDateEntryScreen()
  fun showInvalidDateError()
  fun showDateIsInFutureError()
  fun hideDateErrorMessage()
  fun setDateOnInputFields(date: LocalDate)
  fun showBloodSugarDate(date: LocalDate)
  fun showRemoveButton()
  fun hideRemoveButton()
  fun setBloodSugarReading(bloodSugarReading: String)
  fun dismiss()
  fun showConfirmRemoveBloodSugarDialog(bloodSugarMeasurementUuid: UUID)
  fun showEntryTitle(measurementType: BloodSugarMeasurementType)
  fun showEditTitle(measurementType: BloodSugarMeasurementType)
  fun showProgress()
  fun hideProgress()
  fun setBloodSugarUnitPreferenceLabelToMmol()
  fun setBloodSugarUnitPreferenceLabelToMg()
  fun showBloodSugarUnitPreferenceButton()
  fun hideBloodSugarUnitPreferenceButton()
  fun showBloodSugarUnitPreferenceLabel()
  fun hideBloodSugarUnitPreferenceLabel()
  fun decimalOrNumericBloodSugarInputType()
  fun numericBloodSugarInputType()
  fun setLabelForHbA1c()
  fun setLabelForUnknown()
  fun showBloodSugarUnitSelectionDialog(bloodSugarUnitPreference: BloodSugarUnitPreference)
}
