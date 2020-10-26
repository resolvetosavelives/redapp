package org.simple.clinic.scheduleappointment

import org.simple.clinic.facility.Facility
import org.simple.clinic.mobius.ViewRenderer
import org.simple.clinic.newentry.ButtonState
import org.simple.clinic.overdue.PotentialAppointmentDate
import org.simple.clinic.teleconsultlog.teleconsultrecord.TeleconsultStatus
import org.simple.clinic.util.ValueChangedCallback

class ScheduleAppointmentUiRenderer(
    private val ui: ScheduleAppointmentUi
) : ViewRenderer<ScheduleAppointmentModel> {

  private val selectedDateChangedCallback = ValueChangedCallback<PotentialAppointmentDate>()
  private val facilityChangedCallback = ValueChangedCallback<Facility>()

  override fun render(model: ScheduleAppointmentModel) {
    if (model.hasLoadedAppointmentDate) {
      selectedDateChangedCallback.pass(model.selectedAppointmentDate!!) { selectedDate ->
        ui.updateScheduledAppointment(selectedDate.scheduledFor, selectedDate.timeToAppointment)
        toggleStateOfIncrementButton(selectedDate, model.potentialAppointmentDates)
        toggleStateOfDecrementButton(selectedDate, model.potentialAppointmentDates)
      }
    }

    if (model.hasLoadedAppointmentFacility) {
      facilityChangedCallback.pass(model.appointmentFacility!!) { facility ->
        ui.showPatientFacility(facility.name)
      }
    }

    manageDoneAndNextButtonVisibility(model)

    manageButtonState(model)
  }

  private fun manageDoneAndNextButtonVisibility(model: ScheduleAppointmentModel) {
    if (model.teleconsultRecord != null) {
      if (model.teleconsultRecord.teleconsultRequestInfo != null)
        manageRequestCompletedStatus(model)
    } else {
      ui.showDoneButton()
      ui.hideNextButton()
    }
  }

  private fun manageRequestCompletedStatus(model: ScheduleAppointmentModel) {
    when (model.requesterCompletionStatus) {
      TeleconsultStatus.StillWaiting, null -> {
        ui.showNextButton()
        ui.hideDoneButton()
      }
      is TeleconsultStatus.Unknown, TeleconsultStatus.Yes, TeleconsultStatus.No -> {
        ui.showDoneButton()
        ui.hideNextButton()
      }
    }
  }

  private fun manageButtonState(model: ScheduleAppointmentModel) {
    if (model.doneButtonState == ButtonState.SAVING) {
      ui.showProgress()
    } else
      ui.hideProgress()
  }

  private fun toggleStateOfIncrementButton(
      selectedAppointmentDate: PotentialAppointmentDate,
      allPotentialAppointmentDates: List<PotentialAppointmentDate>
  ) {
    val areLaterPotentialAppointmentsAvailable = selectedAppointmentDate < allPotentialAppointmentDates.last()

    ui.enableIncrementButton(areLaterPotentialAppointmentsAvailable)
  }

  private fun toggleStateOfDecrementButton(
      selectedAppointmentDate: PotentialAppointmentDate,
      allPotentialAppointmentDates: List<PotentialAppointmentDate>
  ) {
    val areEarlierPotentialAppointmentsAvailable = selectedAppointmentDate > allPotentialAppointmentDates.first()

    ui.enableDecrementButton(areEarlierPotentialAppointmentsAvailable)
  }
}
