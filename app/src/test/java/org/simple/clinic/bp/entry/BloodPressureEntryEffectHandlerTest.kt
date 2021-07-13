package org.simple.clinic.bp.entry

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.After
import org.junit.Test
import org.simple.clinic.TestData
import org.simple.clinic.mobius.EffectHandlerTestCase
import org.simple.clinic.util.TestUserClock
import org.simple.clinic.util.scheduler.TestSchedulersProvider
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.util.UUID

class BloodPressureEntryEffectHandlerTest {

  private val ui = mock<BloodPressureEntryUi>()
  private val userClock = TestUserClock()

  private val facility = TestData.facility(uuid = UUID.fromString("7a7df523-8397-4c9e-bcef-d0047ea2e969"))
  private val user = TestData.loggedInUser(
      uuid = UUID.fromString("5f8c9705-6732-4d1c-aea3-3b5ab10d4a0e"),
      registrationFacilityUuid = facility.uuid,
      currentFacilityUuid = facility.uuid
  )

  private val effectHandler = BloodPressureEntryEffectHandler(
      ui = ui,
      patientRepository = mock(),
      bloodPressureRepository = mock(),
      appointmentsRepository = mock(),
      userClock = userClock,
      schedulersProvider = TestSchedulersProvider.trampoline(),
      uuidGenerator = mock(),
      currentUser = { user },
      currentFacility = { facility }
  ).build()

  private val testCase = EffectHandlerTestCase(effectHandler)

  @After
  fun teardown() {
    testCase.dispose()
  }

  @Test
  fun `when prefill date is dispatched, then populate date button and date input fields`() {
    // when
    val entryDate = LocalDate.of(1992, 6, 7)
    userClock.setDate(LocalDate.of(1992, 6, 7), UTC)
    testCase.dispatch(PrefillDate.forNewEntry())

    // then
    verify(ui).setDateOnInputFields(entryDate)
    verify(ui).showBpDate(entryDate)
    testCase.assertOutgoingEvents(DatePrefilled(entryDate))
  }
}
