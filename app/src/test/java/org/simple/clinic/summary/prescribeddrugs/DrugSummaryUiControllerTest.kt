package org.simple.clinic.summary.prescribeddrugs

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.ofType
import io.reactivex.subjects.PublishSubject
import org.junit.After
import org.junit.Test
import org.simple.clinic.TestData
import org.simple.clinic.drugs.PrescribedDrug
import org.simple.clinic.drugs.PrescriptionRepository
import org.simple.clinic.facility.FacilityRepository
import org.simple.clinic.user.UserSession
import org.simple.clinic.util.scheduler.TestSchedulersProvider
import org.simple.clinic.widgets.ScreenCreated
import org.simple.clinic.widgets.UiEvent
import org.simple.mobius.migration.MobiusTestFixture
import java.util.UUID

class DrugSummaryUiControllerTest {

  private val patientUuid = UUID.fromString("f5dfca05-59da-4b91-9743-84d2690844c1")

  private val ui = mock<DrugSummaryUi>()
  private val repository = mock<PrescriptionRepository>()
  private val events = PublishSubject.create<UiEvent>()
  private val userSession = mock<UserSession>()
  private val facilityRepository = mock<FacilityRepository>()

  private lateinit var controllerSubscription: Disposable
  private lateinit var testFixture: MobiusTestFixture<DrugSummaryModel, DrugSummaryEvent, DrugSummaryEffect>

  @After
  fun tearDown() {
    controllerSubscription.dispose()
    testFixture.dispose()
  }

  @Test
  fun `patient's prescription summary should be populated`() {
    // given
    val prescriptions = listOf(
        TestData.prescription(
            uuid = UUID.fromString("b99c147c-d2db-4b73-9b8e-cf8866b7daa1"),
            name = "Amlodipine",
            dosage = "10mg"
        ),
        TestData.prescription(
            uuid = UUID.fromString("262752e2-1565-4140-98d8-0b1e914dbb64"),
            name = "Telmisartan",
            dosage = "9000mg"
        ),
        TestData.prescription(
            uuid = UUID.fromString("f0674dbd-6981-4b79-ae3c-8b513ba1166c"),
            name = "Randomzole",
            dosage = "2 packets"
        )
    )
    whenever(repository.newestPrescriptionsForPatient(patientUuid)) doReturn Observable.just(prescriptions)

    // when
    setupController()

    // then
    verify(ui).populatePrescribedDrugs(prescriptions)
    verifyNoMoreInteractions(ui)

    verify(repository).newestPrescriptionsForPatient(patientUuid)
    verifyNoMoreInteractions(repository)

    verifyZeroInteractions(userSession)
    verifyZeroInteractions(facilityRepository)
  }

  @Test
  fun `when update medicines is clicked then updated prescription screen should be shown`() {
    // given
    val loggedInUser = TestData.loggedInUser(UUID.fromString("e83b9b27-0a05-4750-9ef7-270cda65217b"))
    val currentFacility = TestData.facility(UUID.fromString("af8e817c-8772-4c84-9f4f-1f331fa0b2a5"))

    whenever(repository.newestPrescriptionsForPatient(patientUuid)) doReturn Observable.never<List<PrescribedDrug>>()
    whenever(userSession.loggedInUserImmediate()) doReturn loggedInUser
    whenever(facilityRepository.currentFacilityImmediate(loggedInUser)) doReturn currentFacility

    // when
    setupController()
    events.onNext(PatientSummaryUpdateDrugsClicked())

    // then
    verify(ui).showUpdatePrescribedDrugsScreen(patientUuid, currentFacility)
    verifyNoMoreInteractions(ui)

    verify(repository).newestPrescriptionsForPatient(patientUuid)
    verifyNoMoreInteractions(repository)

    verify(userSession).loggedInUserImmediate()
    verifyNoMoreInteractions(userSession)

    verify(facilityRepository).currentFacilityImmediate(loggedInUser)
    verifyNoMoreInteractions(facilityRepository)
  }

  private fun setupController() {
    val controller = DrugSummaryUiController(patientUuid, repository, facilityRepository, userSession)

    controllerSubscription = events
        .compose(controller)
        .subscribe { it.invoke(ui) }

    events.onNext(ScreenCreated())

    val effectHandler = DrugSummaryEffectHandler(
        prescriptionRepository = repository,
        schedulersProvider = TestSchedulersProvider.trampoline(),
        uiActions = ui
    )
    val uiRenderer = DrugSummaryUiRenderer(ui)

    testFixture = MobiusTestFixture(
        events = events.ofType(),
        defaultModel = DrugSummaryModel.create(patientUuid),
        init = DrugSummaryInit(),
        update = DrugSummaryUpdate(),
        effectHandler = effectHandler.build(),
        modelUpdateListener = uiRenderer::render
    )

    testFixture.start()
  }
}
