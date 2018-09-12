package org.simple.clinic.bp.entry

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.simple.clinic.bp.BloodPressureRepository
import org.simple.clinic.overdue.AppointmentRepository
import org.simple.clinic.widgets.UiEvent
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneOffset
import java.util.UUID

class BloodPressureEntrySheetControllerTest {

  private val sheet = mock<BloodPressureEntrySheet>()
  private val bloodPressureRepository = mock<BloodPressureRepository>()
  private val appointmentRepository = mock<AppointmentRepository>()
  private val patientUuid = UUID.randomUUID()

  private val uiEvents = PublishSubject.create<UiEvent>()
  private lateinit var controller: BloodPressureEntrySheetController

  @Before
  fun setUp() {
    controller = BloodPressureEntrySheetController(bloodPressureRepository, appointmentRepository)

    uiEvents
        .compose(controller)
        .subscribe { uiChange -> uiChange(sheet) }
  }

  @Test
  fun `when save is clicked but input is invalid then blood pressure measurement should not be saved`() {
    uiEvents.onNext(BloodPressureEntrySheetCreated(patientUuid))
    uiEvents.onNext(BloodPressureSystolicTextChanged("1"))
    uiEvents.onNext(BloodPressureDiastolicTextChanged("1"))
    uiEvents.onNext(BloodPressureSaveClicked())

    verify(bloodPressureRepository, never()).saveMeasurement(any(), any(), any())
    verify(appointmentRepository, never()).schedule(any(), any())
    verify(sheet, never()).finishAndScheduleAppointment(patientUuid)
  }

  @Test
  fun `when save is clicked and input is valid then blood pressure measurement should be saved`() {
    whenever(bloodPressureRepository.saveMeasurement(patientUuid, 142, 80)).thenReturn(Completable.complete())
    whenever(appointmentRepository.schedule(patientUuid, LocalDate.now(ZoneOffset.UTC).minusDays(1))).thenReturn(Completable.complete())

    uiEvents.onNext(BloodPressureEntrySheetCreated(patientUuid))
    uiEvents.onNext(BloodPressureSystolicTextChanged("142"))
    uiEvents.onNext(BloodPressureDiastolicTextChanged("80"))
    uiEvents.onNext(BloodPressureSaveClicked())
    uiEvents.onNext(BloodPressureSaveClicked())
    uiEvents.onNext(BloodPressureSaveClicked())

    verify(bloodPressureRepository, times(1)).saveMeasurement(patientUuid, 142, 80)
    verify(appointmentRepository, times(1)).schedule(patientUuid, LocalDate.now(ZoneOffset.UTC).minusDays(1))
    verify(sheet).finishAndScheduleAppointment(patientUuid)
  }
}
