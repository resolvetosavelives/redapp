package org.simple.clinic.bp.entry

import java.time.LocalDate
import java.util.UUID

// TODO(rj) 17/Oct/19 - Move some of these events to a UiActions interface after migrating to Mobius?
interface BloodPressureEntryUi {
  fun changeFocusToDiastolic()
  fun changeFocusToSystolic()
  fun setBpSavedResultAndFinish()
  fun hideBpErrorMessage()
  fun showSystolicLessThanDiastolicError()
  fun showSystolicLowError()
  fun showSystolicHighError()
  fun showDiastolicLowError()
  fun showDiastolicHighError()
  fun showSystolicEmptyError()
  fun showDiastolicEmptyError()
  fun setSystolic(systolic: String)
  fun setDiastolic(diastolic: String)
  fun showRemoveBpButton()
  fun hideRemoveBpButton()
  fun showEnterNewBloodPressureTitle()
  fun showEditBloodPressureTitle()
  fun showConfirmRemoveBloodPressureDialog(uuid: UUID)
  fun showBpEntryScreen()
  fun showDateEntryScreen()
  fun showInvalidDateError()
  fun showDateIsInFutureError()
  fun hideDateErrorMessage()
  fun setDateOnInputFields(date: LocalDate)
  fun showBpDate(date: LocalDate)
  fun dismiss()
  fun showProgress()
  fun hideProgress()
}
