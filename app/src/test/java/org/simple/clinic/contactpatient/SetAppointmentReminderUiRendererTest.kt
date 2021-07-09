package org.simple.clinic.contactpatient

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.Test
import org.simple.clinic.overdue.AppointmentConfig
import org.simple.clinic.overdue.PotentialAppointmentDate
import org.simple.clinic.overdue.TimeToAppointment
import org.simple.clinic.overdue.TimeToAppointment.Days
import org.simple.clinic.overdue.TimeToAppointment.Weeks
import org.simple.clinic.util.TestUserClock
import java.time.LocalDate
import java.time.Period
import java.util.UUID

class SetAppointmentReminderUiRendererTest {

  private val patientUuid = UUID.fromString("e6ff79b9-0ac8-4d7b-ada9-7b3056db2972")

  private val ui = mock<ContactPatientUi>()
  private val timeToAppointments = listOf(
      Days(1),
      Weeks(1),
      Weeks(2)
  )
  private val clock = TestUserClock(LocalDate.parse("2018-01-01"))
  private val uiRenderer = ContactPatientUiRenderer(ui, clock)

  @Test
  fun `the current selected appointment date must be shown long with the reminder periods`() {
    // given
    val reminderPeriod = Weeks(1)
    val selectedReminderDate = PotentialAppointmentDate(
        timeToAppointment = reminderPeriod,
        scheduledFor = LocalDate.parse("2018-01-08")
    )

    // when
    val model = defaultModel().reminderDateSelected(selectedReminderDate)
        .contactPatientInfoLoaded()
    uiRenderer.render(model)

    // then
    verify(ui).hideProgress()
    verify(ui).switchToSetAppointmentReminderView_Old()
    verify(ui).enablePreviousReminderDateStepper()
    verify(ui).enableNextReminderDateStepper()

    verify(ui).renderSelectedAppointmentDate(reminderPeriod, selectedReminderDate.scheduledFor)
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `when the current selected date is the earliest available date, disable the previous date button`() {
    // given
    val reminderPeriod = Days(1)
    val selectedReminderDate = PotentialAppointmentDate(
        timeToAppointment = reminderPeriod,
        scheduledFor = LocalDate.parse("2018-01-02")
    )

    // when
    val model = defaultModel().reminderDateSelected(selectedReminderDate)
        .contactPatientInfoLoaded()
    uiRenderer.render(model)

    // then
    verify(ui).hideProgress()
    verify(ui).switchToSetAppointmentReminderView_Old()
    verify(ui).renderSelectedAppointmentDate(reminderPeriod, selectedReminderDate.scheduledFor)
    verify(ui).enableNextReminderDateStepper()

    verify(ui).disablePreviousReminderDateStepper()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `when the current selected date is not the earliest available date, enable the previous date button`() {
    // given
    val reminderPeriod = Weeks(1)
    val selectedReminderDate = PotentialAppointmentDate(
        timeToAppointment = reminderPeriod,
        scheduledFor = LocalDate.parse("2018-01-08")
    )

    // when
    val model = defaultModel().reminderDateSelected(selectedReminderDate)
        .contactPatientInfoLoaded()
    uiRenderer.render(model)

    // then
    verify(ui).hideProgress()
    verify(ui).switchToSetAppointmentReminderView_Old()
    verify(ui).renderSelectedAppointmentDate(reminderPeriod, selectedReminderDate.scheduledFor)
    verify(ui).enableNextReminderDateStepper()

    verify(ui).enablePreviousReminderDateStepper()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `when the current selected date is the latest available date, disable the next date button`() {
    // given
    val reminderPeriod = Weeks(2)
    val selectedReminderDate = PotentialAppointmentDate(
        timeToAppointment = reminderPeriod,
        scheduledFor = LocalDate.parse("2018-01-15")
    )

    // when
    val model = defaultModel().reminderDateSelected(selectedReminderDate)
        .contactPatientInfoLoaded()
    uiRenderer.render(model)

    // then
    verify(ui).hideProgress()
    verify(ui).switchToSetAppointmentReminderView_Old()
    verify(ui).renderSelectedAppointmentDate(reminderPeriod, selectedReminderDate.scheduledFor)
    verify(ui).enablePreviousReminderDateStepper()

    verify(ui).disableNextReminderDateStepper()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `when the current selected date is not the latest available date, enable the next date button`() {
    // given
    val reminderPeriod = Weeks(1)
    val selectedReminderDate = PotentialAppointmentDate(
        timeToAppointment = reminderPeriod,
        scheduledFor = LocalDate.parse("2018-01-08")
    )

    // when
    val model = defaultModel().reminderDateSelected(selectedReminderDate)
        .contactPatientInfoLoaded()
    uiRenderer.render(model)

    // then
    verify(ui).hideProgress()
    verify(ui).switchToSetAppointmentReminderView_Old()
    verify(ui).renderSelectedAppointmentDate(reminderPeriod, selectedReminderDate.scheduledFor)
    verify(ui).enablePreviousReminderDateStepper()

    verify(ui).enableNextReminderDateStepper()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `when the current selected date is not in the list of available dates, set the reminder period as the total number of days`() {
    // given
    val reminderPeriod = Days(3)
    val selectedReminderDate = PotentialAppointmentDate(
        timeToAppointment = reminderPeriod,
        scheduledFor = LocalDate.parse("2018-01-04")
    )

    // when
    val model = defaultModel().reminderDateSelected(selectedReminderDate)
        .contactPatientInfoLoaded()
    uiRenderer.render(model)

    // then
    verify(ui).hideProgress()
    verify(ui).switchToSetAppointmentReminderView_Old()
    verify(ui).renderSelectedAppointmentDate(reminderPeriod, selectedReminderDate.scheduledFor)
    verify(ui).enablePreviousReminderDateStepper()

    verify(ui).enableNextReminderDateStepper()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `when overdue list changes feature flag is true, then the current selected appointment date must be shown long with the reminder periods with the new view`() {
    // given
    val reminderPeriod = Weeks(1)
    val selectedReminderDate = PotentialAppointmentDate(
        timeToAppointment = reminderPeriod,
        scheduledFor = LocalDate.parse("2018-01-08")
    )

    // when
    val model = defaultModel(overdueListChangesFeatureEnabled = true).reminderDateSelected(selectedReminderDate)
        .contactPatientInfoLoaded()
    uiRenderer.render(model)

    // then
    verify(ui).hideProgress()
    verify(ui).switchToSetAppointmentReminderView()
    verify(ui).enablePreviousReminderDateStepper()
    verify(ui).enableNextReminderDateStepper()
    verify(ui).renderSelectedAppointmentDate(reminderPeriod, selectedReminderDate.scheduledFor)
    verifyNoMoreInteractions(ui)

  }

  private fun defaultModel(
      phoneMaskFeatureEnabled: Boolean = false,
      timeToAppointments: List<TimeToAppointment> = this.timeToAppointments,
      overdueListChangesFeatureEnabled: Boolean = false
  ): ContactPatientModel {
    val appointmentConfig = AppointmentConfig(
        appointmentDuePeriodForDefaulters = Period.ZERO,
        scheduleAppointmentsIn = emptyList(),
        defaultTimeToAppointment = Days(0),
        periodForIncludingOverdueAppointments = Period.ZERO,
        remindAppointmentsIn = timeToAppointments
    )

    return ContactPatientModel.create(
        patientUuid = patientUuid,
        appointmentConfig = appointmentConfig,
        userClock = clock,
        mode = UiMode.SetAppointmentReminder,
        secureCallFeatureEnabled = phoneMaskFeatureEnabled,
        overdueListChangesFeatureEnabled = overdueListChangesFeatureEnabled
    )
  }
}
