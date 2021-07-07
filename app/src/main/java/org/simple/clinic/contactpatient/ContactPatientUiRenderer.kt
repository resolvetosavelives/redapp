package org.simple.clinic.contactpatient

import org.simple.clinic.contactpatient.UiMode.CallPatient
import org.simple.clinic.contactpatient.UiMode.SetAppointmentReminder
import org.simple.clinic.home.overdue.OverdueAppointment
import org.simple.clinic.home.overdue.OverduePatientAddress
import org.simple.clinic.mobius.ViewRenderer
import org.simple.clinic.overdue.PotentialAppointmentDate
import org.simple.clinic.overdue.TimeToAppointment
import org.simple.clinic.patient.DateOfBirth
import org.simple.clinic.patient.PatientProfile
import org.simple.clinic.util.ParcelableOptional
import org.simple.clinic.util.UserClock
import org.simple.clinic.util.daysTill
import java.time.LocalDate

class ContactPatientUiRenderer(
    private val ui: ContactPatientUi,
    private val clock: UserClock
) : ViewRenderer<ContactPatientModel> {

  override fun render(model: ContactPatientModel) {
    if (!model.isPatientContactInfoLoaded) {
      ui.showProgress()
      return
    }

    ui.hideProgress()
    when (model.uiMode) {
      CallPatient -> renderCallPatientViewBasedOnFeatureFlag(model)
      SetAppointmentReminder -> renderSetAppointmentReminderViewBasedOnFeatureFlag(model)
    }
  }

  private fun renderCallPatientViewBasedOnFeatureFlag(model: ContactPatientModel) {
    if (model.overdueListChangesFeatureEnabled) {
      renderCallPatientView(model)
    } else {
      renderCallPatientView_Old(model)
    }
  }

  private fun renderSetAppointmentReminderViewBasedOnFeatureFlag(model: ContactPatientModel) {
    if (model.overdueListChangesFeatureEnabled) {
      renderSetAppointmentReminderView(model)
    } else {
      renderSetAppointmentReminderView_Old(model)
    }
  }

  private fun renderSetAppointmentReminderView_Old(model: ContactPatientModel) {
    renderSelectedAppointmentDate(model)
    toggleStateOfReminderDateSteppers(model)

    ui.switchToSetAppointmentReminderView_Old()
  }

  private fun renderSetAppointmentReminderView(model: ContactPatientModel) {
    renderSelectedAppointmentDate(model)
    toggleStateOfReminderDateSteppers(model)

    ui.switchToSetAppointmentReminderView()
  }

  private fun renderCallPatientView_Old(model: ContactPatientModel) {
    if (model.hasLoadedPatientProfile) {
      renderPatientProfile_Old(model.patientProfile!!)
    }

    if (model.hasLoadedAppointment) {
      toggleCallResultSection(model.appointment!!)
    }

    if (model.secureCallingFeatureEnabled) {
      ui.showSecureCallUi_Old()
    } else {
      ui.hideSecureCallUi_Old()
    }

    ui.switchToCallPatientView_Old()
  }

  private fun renderCallPatientView(model: ContactPatientModel) {
    if (model.hasLoadedPatientProfile && model.hasLoadedAppointment) {
      renderPatientProfile(model.patientProfile!!, model.appointment!!)
    }

    if (model.patientProfileHasPhoneNumber && model.hasLoadedAppointment) {
      ui.showPatientWithPhoneNumberUi()
      ui.hidePatientWithNoPhoneNumberUi()

      loadSecureCallingUi(model)
    } else {
      ui.showPatientWithNoPhoneNumberUi()
      ui.hidePatientWithPhoneNumberUi()
    }

    ui.switchToCallPatientView()
  }

  private fun loadSecureCallingUi(model: ContactPatientModel) {
    if (model.secureCallingFeatureEnabled)
      ui.showSecureCallUi()
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
    val exactlyMatchingReminderPeriod = findReminderPeriodExactlyMatchingDate(model.potentialAppointments, model.selectedAppointmentDate)
    val selectedReminderPeriod = exactlyMatchingReminderPeriod
        ?: daysUntilTodayFrom(model.selectedAppointmentDate)

    ui.renderSelectedAppointmentDate(
        selectedReminderPeriod,
        model.selectedAppointmentDate
    )
  }

  private fun daysUntilTodayFrom(date: LocalDate): TimeToAppointment.Days {
    val today = LocalDate.now(clock)
    return TimeToAppointment.Days(today daysTill date)
  }

  private fun findReminderPeriodExactlyMatchingDate(
      potentialAppointmentDates: List<PotentialAppointmentDate>,
      date: LocalDate
  ): TimeToAppointment? {
    return potentialAppointmentDates.firstOrNull { it.scheduledFor == date }?.timeToAppointment
  }

  private fun toggleCallResultSection(appointment: ParcelableOptional<OverdueAppointment>) {
    if (appointment.isEmpty()) {
      ui.hideCallResultSection_Old()
    } else {
      ui.showCallResultSection_Old()
    }
  }

  private fun renderPatientProfile_Old(
      patientProfile: PatientProfile
  ) {
    val patientAge = DateOfBirth.fromPatient(patientProfile.patient, clock).estimateAge(clock)

    ui.renderPatientDetails_Old(
        name = patientProfile.patient.fullName,
        gender = patientProfile.patient.gender,
        age = patientAge,
        phoneNumber = patientProfile.phoneNumbers.first().number
    )
  }

  private fun renderPatientProfile(
      patientProfile: PatientProfile,
      appointment: ParcelableOptional<OverdueAppointment>
  ) {
    val patientAge = DateOfBirth.fromPatient(patientProfile.patient, clock).estimateAge(clock)

    ui.renderPatientDetails(PatientDetails(
        name = patientProfile.patient.fullName,
        gender = patientProfile.patient.gender,
        age = patientAge,
        phoneNumber = patientProfile.phoneNumbers.firstOrNull()?.number,
        patientAddress = patientAddressText(appointment.get().patientAddress),
        registeredFacility = appointment.get().appointmentFacilityName.orEmpty(),
        diagnosedWithDiabetes = appointment.get().diagnosedWithDiabetes,
        diagnosedWithHypertension = appointment.get().diagnosedWithHypertension,
        lastVisited = appointment.get().patientLastSeen
    ))
  }

  private fun patientAddressText(patientAddress: OverduePatientAddress) = when {
    !patientAddress.streetAddress.isNullOrBlank() && !patientAddress.colonyOrVillage.isNullOrBlank() -> {
      "${patientAddress.streetAddress}, ${patientAddress.colonyOrVillage}"
    }
    !patientAddress.streetAddress.isNullOrBlank() -> patientAddress.streetAddress
    !patientAddress.colonyOrVillage.isNullOrBlank() -> patientAddress.colonyOrVillage
    else -> "${patientAddress.district}, ${patientAddress.state}"
  }
}
