package org.simple.clinic.contactpatient

import org.simple.clinic.overdue.TimeToAppointment
import org.simple.clinic.patient.Gender
import java.time.LocalDate

interface ContactPatientUi {
  fun switchToCallPatientView_Old()
  fun switchToCallPatientView()

  fun switchToSetAppointmentReminderView_Old()
  fun switchToSetAppointmentReminderView()

  fun renderPatientDetails_Old(name: String, gender: Gender, age: Int, phoneNumber: String)
  fun renderPatientDetails(patientDetails: PatientDetails)

  fun showCallResultSection_Old()
  fun hideCallResultSection_Old()
  
  fun showSecureCallUi_Old()
  fun showSecureCallUi()

  fun hideSecureCallUi_Old()
  fun hideSecureCallUi()

  fun showPatientWithNoPhoneNumberUi()
  fun hidePatientWithNoPhoneNumberUi()

  fun showPatientWithPhoneNumberUi()
  fun hidePatientWithPhoneNumberUi()

  fun setResultOfCallLabelText(resultLabel: ResultLabel)

  fun renderSelectedAppointmentDate(
      selectedAppointmentReminderPeriod: TimeToAppointment,
      selectedDate: LocalDate
  )

  fun disablePreviousReminderDateStepper()
  fun enablePreviousReminderDateStepper()
  fun disableNextReminderDateStepper()
  fun enableNextReminderDateStepper()
  fun showProgress()
  fun hideProgress()
}
