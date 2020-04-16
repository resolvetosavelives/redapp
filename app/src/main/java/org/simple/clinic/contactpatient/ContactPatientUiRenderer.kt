package org.simple.clinic.contactpatient

import org.simple.clinic.contactpatient.UiMode.CallPatient
import org.simple.clinic.contactpatient.UiMode.SetAppointmentReminder
import org.simple.clinic.home.overdue.OverdueAppointment
import org.simple.clinic.mobius.ViewRenderer
import org.simple.clinic.patient.DateOfBirth
import org.simple.clinic.patient.PatientProfile
import org.simple.clinic.util.ParcelableOptional
import org.simple.clinic.util.UserClock

class ContactPatientUiRenderer(
    private val ui: ContactPatientUi,
    private val clock: UserClock
) : ViewRenderer<ContactPatientModel> {

  override fun render(model: ContactPatientModel) {
    when (model.uiMode) {
      CallPatient -> renderCallPatientView(model)
      SetAppointmentReminder -> renderSetAppointmentReminderView(model)
    }
  }

  private fun renderSetAppointmentReminderView(model: ContactPatientModel) {
    renderSelectedAppointmentDate(model)
    toggleStateOfReminderDateSteppers(model)

    ui.switchToSetAppointmentReminderView()
  }

  private fun renderCallPatientView(model: ContactPatientModel) {
    if (model.hasLoadedPatientProfile) {
      renderPatientProfile(model.patientProfile!!)
    }

    if (model.hasLoadedAppointment) {
      toggleCallResultSection(model.appointment!!)
    }

    if (model.secureCallingFeatureEnabled) {
      ui.showSecureCallUi()
    } else {
      ui.hideSecureCallUi()
    }

    ui.switchToCallPatientView()
  }

  private fun toggleStateOfReminderDateSteppers(model: ContactPatientModel) {
    val earliestAvailableReminderDate = model.potentialAppointments.first().scheduledFor
    val latestAvailableReminderDate = model.potentialAppointments.last().scheduledFor

    if (model.selectedAppointmentDate == earliestAvailableReminderDate) {
      ui.disablePreviousReminderDateStepper()
    } else {
      ui.enablePreviousReminderDateStepper()
    }

    if (model.selectedAppointmentDate == latestAvailableReminderDate) {
      ui.disableNextReminderDateStepper()
    } else {
      ui.enableNextReminderDateStepper()
    }
  }

  private fun renderSelectedAppointmentDate(
      model: ContactPatientModel
  ) {
    val appointmentReminderPeriods = model.potentialAppointments.map { it.timeToAppointment }
    val selectedReminderPeriod = model.potentialAppointments.first { it.scheduledFor == model.selectedAppointmentDate }.timeToAppointment

    ui.renderSelectedAppointmentDate(
        appointmentReminderPeriods,
        selectedReminderPeriod,
        model.selectedAppointmentDate
    )
  }

  private fun toggleCallResultSection(appointment: ParcelableOptional<OverdueAppointment>) {
    if (appointment.isEmpty()) {
      ui.hideCallResultSection()
    } else {
      ui.showCallResultSection()
    }
  }

  private fun renderPatientProfile(patientProfile: PatientProfile) {
    val patientAge = DateOfBirth.fromPatient(patientProfile.patient, clock).estimateAge(clock)

    ui.renderPatientDetails(
        name = patientProfile.patient.fullName,
        gender = patientProfile.patient.gender,
        age = patientAge,
        phoneNumber = patientProfile.phoneNumbers.first().number
    )
  }
}
