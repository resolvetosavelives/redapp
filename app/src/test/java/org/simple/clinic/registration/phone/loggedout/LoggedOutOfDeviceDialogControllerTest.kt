package org.simple.clinic.registration.phone.loggedout

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.subjects.PublishSubject
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.simple.clinic.user.UserSession
import org.simple.clinic.user.UserSession.LogoutResult.Failure
import org.simple.clinic.user.UserSession.LogoutResult.Success
import org.simple.clinic.util.RxErrorsRule
import org.simple.clinic.widgets.ScreenCreated
import org.simple.clinic.widgets.UiEvent

class LoggedOutOfDeviceDialogControllerTest {

  @get:Rule
  val rule: TestRule = RxErrorsRule()

  private val dialog = mock<LoggedOutOfDeviceDialog>()
  private val userSession = mock<UserSession>()
  private val uiEvents = PublishSubject.create<UiEvent>()

  private lateinit var controllerSubscription: Disposable

  @After
  fun tearDown() {
    controllerSubscription.dispose()
  }

  @Test
  fun `when the dialog is created, the okay button must be disabled`() {
    // given
    RxJavaPlugins.setErrorHandler(null)
    whenever(userSession.logout()).thenReturn(Single.never())

    // when
    setupController()

    // then
    verify(dialog).disableOkayButton()
    verifyNoMoreInteractions(dialog)
  }

  @Test
  fun `when the logout result completes successfully, the okay button must be enabled`() {
    // given
    RxJavaPlugins.setErrorHandler(null)
    whenever(userSession.logout()).thenReturn(Single.just(Success))

    // when
    setupController()

    // then
    verify(dialog).disableOkayButton()
    verify(dialog).enableOkayButton()
    verifyNoMoreInteractions(dialog)
  }

  @Test
  fun `when the logout fails with runtime exception, then error must be thrown`() {
    // given
    var thrownError: Throwable? = null
    RxJavaPlugins.setErrorHandler { thrownError = it }
    whenever(userSession.logout()).thenReturn(Single.just(Failure(RuntimeException())))

    // when
    setupController()

    // then
    verify(dialog).disableOkayButton()
    verify(dialog, never()).enableOkayButton()
    verifyNoMoreInteractions(dialog)
    assertThat(thrownError).isNotNull()
  }

  @Test
  fun `when the logout fails with null pointer exception, then error must be thrown`() {
    // given
    var thrownError: Throwable? = null
    RxJavaPlugins.setErrorHandler { thrownError = it }
    whenever(userSession.logout()).thenReturn(Single.just(Failure(NullPointerException())))

    // when
    setupController()

    // then
    verify(dialog).disableOkayButton()
    verify(dialog, never()).enableOkayButton()
    verifyNoMoreInteractions(dialog)
    assertThat(thrownError).isNotNull()
  }

  private fun setupController() {
    val controller = LoggedOutOfDeviceDialogController(userSession)

    controllerSubscription = uiEvents
        .compose(controller)
        .subscribe({ uiChange -> uiChange(dialog) }, { throw it })

    uiEvents.onNext(ScreenCreated())
  }
}
