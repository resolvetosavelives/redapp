package org.simple.clinic.contactpatient

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.simple.clinic.contactpatient.ContactPatientInfoProgressState.DONE
import org.simple.clinic.contactpatient.ContactPatientInfoProgressState.IN_PROGRESS
import org.simple.clinic.home.overdue.OverdueAppointment
import org.simple.clinic.overdue.AppointmentConfig
import org.simple.clinic.overdue.PotentialAppointmentDate
import org.simple.clinic.patient.PatientProfile
import org.simple.clinic.util.ParcelableOptional
import org.simple.clinic.util.UserClock
import org.simple.clinic.util.parcelable
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@Parcelize
data class ContactPatientModel(
    val patientUuid: UUID,
    val uiMode: UiMode,
    val patientProfile: PatientProfile? = null,
    val appointment: ParcelableOptional<OverdueAppointment>? = null,
    val secureCallingFeatureEnabled: Boolean,
    val potentialAppointments: List<PotentialAppointmentDate>,
    val selectedAppointmentDate: LocalDate,
    val selectedRemoveAppointmentReason: RemoveAppointmentReason?,
    val contactPatientInfoProgressState: ContactPatientInfoProgressState?,
    val overdueListChangesFeatureEnabled: Boolean
) : Parcelable {

  companion object {
    fun create(
        patientUuid: UUID,
        appointmentConfig: AppointmentConfig,
        userClock: UserClock,
        mode: UiMode,
        secureCallFeatureEnabled: Boolean,
        overdueListChangesFeatureEnabled: Boolean
    ): ContactPatientModel {
      val potentialAppointments = PotentialAppointmentDate.from(appointmentConfig.remindAppointmentsIn, userClock)

      return ContactPatientModel(
          patientUuid = patientUuid,
          uiMode = mode,
          secureCallingFeatureEnabled = secureCallFeatureEnabled,
          potentialAppointments = potentialAppointments,
          selectedAppointmentDate = potentialAppointments.first().scheduledFor,
          selectedRemoveAppointmentReason = null,
          contactPatientInfoProgressState = null,
          overdueListChangesFeatureEnabled = overdueListChangesFeatureEnabled
      )
    }
  }

  val hasLoadedPatientProfile: Boolean
    get() = patientProfile != null

  val patientProfileHasPhoneNumber: Boolean
    get() = patientProfile != null && !patientProfile.phoneNumbers.isNullOrEmpty()

  val hasLoadedAppointment: Boolean
    get() = appointment != null

  val hasSelectedARemoveAppointmentReason: Boolean
    get() = selectedRemoveAppointmentReason != null

  val appointmentUuid: UUID
    get() = appointment!!.get().appointment.uuid

  val isPatientContactInfoLoaded: Boolean
    get() = contactPatientInfoProgressState == DONE

  fun patientProfileLoaded(patientProfile: PatientProfile): ContactPatientModel {
    return copy(patientProfile = patientProfile)
  }

  fun overdueAppointmentLoaded(appointment: Optional<OverdueAppointment>): ContactPatientModel {
    return copy(appointment = appointment.parcelable())
  }

  fun reminderDateSelected(date: PotentialAppointmentDate): ContactPatientModel {
    return copy(selectedAppointmentDate = date.scheduledFor)
  }

  fun changeUiModeTo(newMode: UiMode): ContactPatientModel {
    return copy(uiMode = newMode)
  }

  fun removeAppointmentReasonSelected(reason: RemoveAppointmentReason): ContactPatientModel {
    return copy(selectedRemoveAppointmentReason = reason)
  }

  fun contactPatientInfoLoaded(): ContactPatientModel {
    return copy(contactPatientInfoProgressState = DONE)
  }

  fun contactPatientInfoLoading(): ContactPatientModel {
    return copy(contactPatientInfoProgressState = IN_PROGRESS)
  }
}
