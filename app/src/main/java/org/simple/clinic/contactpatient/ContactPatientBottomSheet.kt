package org.simple.clinic.contactpatient

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.transition.TransitionManager
import com.google.android.material.transition.MaterialFade
import com.google.android.material.transition.MaterialSharedAxis
import io.reactivex.Observable
import io.reactivex.rxkotlin.cast
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.parcelize.Parcelize
import org.simple.clinic.ReportAnalyticsEvents
import org.simple.clinic.databinding.SheetContactPatientBinding
import org.simple.clinic.datepicker.DatePickerKeyFactory
import org.simple.clinic.datepicker.DatePickerResult
import org.simple.clinic.datepicker.SelectedDate
import org.simple.clinic.di.injector
import org.simple.clinic.feature.Feature.OverdueListChanges
import org.simple.clinic.feature.Feature.SecureCalling
import org.simple.clinic.feature.Features
import org.simple.clinic.navigation.v2.ExpectsResult
import org.simple.clinic.navigation.v2.Router
import org.simple.clinic.navigation.v2.ScreenKey
import org.simple.clinic.navigation.v2.ScreenResult
import org.simple.clinic.navigation.v2.Succeeded
import org.simple.clinic.navigation.v2.fragments.BaseBottomSheet
import org.simple.clinic.overdue.AppointmentConfig
import org.simple.clinic.overdue.TimeToAppointment
import org.simple.clinic.patient.Gender
import org.simple.clinic.phone.Dialer
import org.simple.clinic.phone.PhoneCaller
import org.simple.clinic.phone.PhoneNumberMaskerConfig
import org.simple.clinic.removeoverdueappointment.RemoveOverdueAppointmentScreen
import org.simple.clinic.router.screen.ActivityPermissionResult
import org.simple.clinic.util.RequestPermissions
import org.simple.clinic.util.RuntimePermissions
import org.simple.clinic.util.UserClock
import org.simple.clinic.util.onBackPressed
import org.simple.clinic.util.toLocalDateAtZone
import org.simple.clinic.util.unsafeLazy
import org.simple.clinic.util.valueOrEmpty
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

class ContactPatientBottomSheet : BaseBottomSheet<
    ContactPatientBottomSheet.Key,
    SheetContactPatientBinding,
    ContactPatientModel,
    ContactPatientEvent,
    ContactPatientEffect>(), ContactPatientUi, ContactPatientUiActions, ExpectsResult {

  @Inject
  lateinit var phoneCaller: PhoneCaller

  @Inject
  lateinit var locale: Locale

  @Inject
  lateinit var phoneMaskConfig: PhoneNumberMaskerConfig

  @Inject
  lateinit var effectHandlerFactory: ContactPatientEffectHandler.Factory

  @Inject
  lateinit var userClock: UserClock

  @Inject
  lateinit var runtimePermissions: RuntimePermissions

  @Inject
  lateinit var appointmentConfig: AppointmentConfig

  @Inject
  @Named("date_for_user_input")
  lateinit var dateTimeFormatter: DateTimeFormatter

  @Inject
  lateinit var features: Features

  @Inject
  lateinit var router: Router

  @Inject
  lateinit var datePickerKeyFactory: DatePickerKeyFactory

  private val patientUuid by unsafeLazy { screenKey.patientId }

  private val permissionResults: Subject<ActivityPermissionResult> = PublishSubject.create()

  private val hotEvents: PublishSubject<ContactPatientEvent> = PublishSubject.create()

  private val contentFlipper
    get() = binding.contentFlipper

  private val callPatientView_Old
    get() = binding.callPatientViewOld

  private val callPatientView
    get() = binding.callPatientView

  private val setAppointmentReminderView
    get() = binding.setAppointmentReminderView

  private val progressIndicator
    get() = binding.progressIndicator

  override fun defaultModel() = ContactPatientModel.create(
      patientUuid = patientUuid,
      appointmentConfig = appointmentConfig,
      userClock = userClock,
      mode = UiMode.CallPatient,
      secureCallFeatureEnabled = features.isEnabled(SecureCalling) && phoneMaskConfig.proxyPhoneNumber.isNotBlank(),
      overdueListChangesFeatureEnabled = features.isEnabled(OverdueListChanges)
  )

  override fun bindView(inflater: LayoutInflater, container: ViewGroup?) =
      SheetContactPatientBinding.inflate(inflater, container, false)

  override fun uiRenderer() = ContactPatientUiRenderer(this, userClock)

  override fun events() = Observable
      .mergeArray(
          normalCallClicks_Old(),
          secureCallClicks_Old(),
          agreedToVisitClicks_Old(),
          remindToCallLaterClicks_Old(),
          nextReminderDateClicks(),
          previousReminderDateClicks(),
          appointmentDateClicks(),
          saveReminderDateClicks(),
          removeFromOverdueListClicks_Old(),
          hotEvents
      )
      .compose(RequestPermissions<ContactPatientEvent>(runtimePermissions, permissionResults))
      .compose(ReportAnalyticsEvents())
      .cast<ContactPatientEvent>()

  override fun createUpdate() = ContactPatientUpdate(phoneMaskConfig)

  override fun createInit() = ContactPatientInit()

  override fun createEffectHandler() = effectHandlerFactory.create(this)
      .build()

  override fun onAttach(context: Context) {
    super.onAttach(context)

    context.injector<Injector>().inject(this)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return super.onCreateDialog(savedInstanceState).apply {
      onBackPressed(::backPressed)
    }
  }

  override fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<out String>,
      grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    permissionResults.onNext(ActivityPermissionResult(requestCode))
  }

  override fun onScreenResult(requestType: Parcelable, result: ScreenResult) {
    if (result !is Succeeded) return

    when (requestType) {
      DatePickerResult -> {
        val selectedDate = result.result as SelectedDate
        val event = ManualDateSelected(selectedDate = selectedDate.date, currentDate = LocalDate.now(userClock))
        hotEvents.onNext(event)
      }
      RemoveOverdueAppointmentResult -> {
        router.pop()
      }
    }
  }

  override fun renderPatientDetails_Old(name: String, gender: Gender, age: Int, phoneNumber: String) {
    callPatientView_Old.renderPatientDetails(name, gender, age, phoneNumber)
  }

  override fun renderPatientDetails(patientDetails: PatientDetails) {
    callPatientView.renderPatientDetails(PatientDetails(
        patientDetails.name,
        patientDetails.gender,
        patientDetails.age,
        patientDetails.phoneNumber.valueOrEmpty(),
        patientDetails.patientAddress,
        patientDetails.registeredFacility,
        patientDetails.diagnosedWithDiabetes,
        patientDetails.diagnosedWithHypertension,
        patientDetails.lastVisited
    ),
        dateTimeFormatter.format(patientDetails.lastVisited.toLocalDateAtZone(userClock.zone)))
  }

  override fun showCallResultSection_Old() {
    fadeIn(contentFlipper)

    callPatientView_Old.callResultSectionVisible = true
  }

  override fun hideCallResultSection_Old() {
    fadeIn(contentFlipper)

    callPatientView_Old.callResultSectionVisible = false
  }

  override fun showSecureCallUi_Old() {
    callPatientView_Old.secureCallingSectionVisible = true
  }

  override fun showSecureCallUi() {
    callPatientView.secureCallingSectionVisible = true
  }

  override fun hideSecureCallUi_Old() {
    callPatientView_Old.secureCallingSectionVisible = false
  }

  override fun hideSecureCallUi() {
    callPatientView.secureCallingSectionVisible = false
  }

  override fun showPatientWithNoPhoneNumberUi() {
    callPatientView.showPatientWithNoPhoneNumberLayout = true
  }

  override fun hidePatientWithNoPhoneNumberUi() {
    callPatientView.showPatientWithNoPhoneNumberLayout = false
  }

  override fun showPatientWithPhoneNumberUi() {
    callPatientView.showPatientWithPhoneNumberLayout = true
  }

  override fun hidePatientWithPhoneNumberUi() {
    callPatientView.showPatientWithPhoneNumberLayout = false
  }

  override fun directlyCallPatient(patientPhoneNumber: String, dialer: Dialer) {
    phoneCaller.normalCall(patientPhoneNumber, dialer)
  }

  override fun maskedCallPatient(patientPhoneNumber: String, proxyNumber: String, dialer: Dialer) {
    phoneCaller.secureCall(
        visibleNumber = proxyNumber,
        hiddenNumber = patientPhoneNumber,
        dialer = dialer
    )
  }

  override fun closeSheet() {
    router.pop()
  }

  override fun renderSelectedAppointmentDate(
      selectedAppointmentReminderPeriod: TimeToAppointment,
      selectedDate: LocalDate
  ) {
    setAppointmentReminderView.renderSelectedAppointmentDate(selectedAppointmentReminderPeriod, selectedDate)
  }

  override fun showManualDatePicker(
      preselectedDate: LocalDate,
      dateBounds: ClosedRange<LocalDate>
  ) {
    val key = datePickerKeyFactory.key(
        preselectedDate = preselectedDate,
        allowedDateRange = dateBounds
    )

    router.pushExpectingResult(DatePickerResult, key)
  }

  override fun disablePreviousReminderDateStepper() {
    setAppointmentReminderView.disablePreviousReminderDateStepper()
  }

  override fun enablePreviousReminderDateStepper() {
    setAppointmentReminderView.enablePreviousReminderDateStepper()
  }

  override fun disableNextReminderDateStepper() {
    setAppointmentReminderView.disableNextReminderDateStepper()
  }

  override fun enableNextReminderDateStepper() {
    setAppointmentReminderView.enableNextReminderDateStepper()
  }

  override fun switchToCallPatientView_Old() {
    callPatientView_Old.visibility = VISIBLE
    setAppointmentReminderView.visibility = GONE
  }

  override fun switchToCallPatientView() {
    callPatientView.visibility = VISIBLE
    setAppointmentReminderView.visibility = GONE
  }

  override fun switchToSetAppointmentReminderView_Old() {
    sharedAxis(contentFlipper)

    callPatientView_Old.visibility = GONE
    setAppointmentReminderView.visibility = VISIBLE
  }

  override fun switchToSetAppointmentReminderView() {
    sharedAxis(contentFlipper)

    callPatientView.visibility = GONE
    setAppointmentReminderView.visibility = VISIBLE  }

  override fun openRemoveOverdueAppointmentScreen(appointmentId: UUID, patientId: UUID) {
    router.pushExpectingResult(
        RemoveOverdueAppointmentResult,
        RemoveOverdueAppointmentScreen.Key(appointmentId, patientId)
    )
  }

  override fun showProgress() {
    progressIndicator.visibility = VISIBLE
  }

  override fun hideProgress() {
    progressIndicator.visibility = GONE
  }

  private fun backPressed() {
    hotEvents.onNext(BackClicked)
  }

  private fun normalCallClicks_Old(): Observable<ContactPatientEvent> {
    return Observable.create { emitter ->
      emitter.setCancellable { callPatientView_Old.normalCallButtonClicked = null }

      callPatientView_Old.normalCallButtonClicked = { emitter.onNext(NormalCallClicked()) }
    }
  }

  private fun secureCallClicks_Old(): Observable<ContactPatientEvent> {
    return Observable.create { emitter ->
      emitter.setCancellable { callPatientView_Old.secureCallButtonClicked = null }

      callPatientView_Old.secureCallButtonClicked = { emitter.onNext(SecureCallClicked()) }
    }
  }

  private fun agreedToVisitClicks_Old(): Observable<ContactPatientEvent> {
    return Observable.create { emitter ->
      emitter.setCancellable { callPatientView_Old.agreedToVisitClicked = null }

      callPatientView_Old.agreedToVisitClicked = { emitter.onNext(PatientAgreedToVisitClicked) }
    }
  }

  private fun remindToCallLaterClicks_Old(): Observable<ContactPatientEvent> {
    return Observable.create { emitter ->
      emitter.setCancellable { callPatientView_Old.remindToCallLaterClicked = null }

      callPatientView_Old.remindToCallLaterClicked = { emitter.onNext(RemindToCallLaterClicked) }
    }
  }

  private fun nextReminderDateClicks(): Observable<ContactPatientEvent> {
    return Observable.create { emitter ->
      emitter.setCancellable { setAppointmentReminderView.incrementStepperClicked = null }

      setAppointmentReminderView.incrementStepperClicked = { emitter.onNext(NextReminderDateClicked) }
    }
  }

  private fun previousReminderDateClicks(): Observable<ContactPatientEvent> {
    return Observable.create { emitter ->
      emitter.setCancellable { setAppointmentReminderView.decrementStepperClicked = null }

      setAppointmentReminderView.decrementStepperClicked = { emitter.onNext(PreviousReminderDateClicked) }
    }
  }

  private fun appointmentDateClicks(): Observable<ContactPatientEvent> {
    return Observable.create { emitter ->
      emitter.setCancellable { setAppointmentReminderView.appointmentDateClicked = null }

      setAppointmentReminderView.appointmentDateClicked = { emitter.onNext(AppointmentDateClicked) }
    }
  }

  private fun saveReminderDateClicks(): Observable<ContactPatientEvent> {
    return Observable.create { emitter ->
      emitter.setCancellable { setAppointmentReminderView.doneClicked = null }

      setAppointmentReminderView.doneClicked = { emitter.onNext(SaveAppointmentReminderClicked) }
    }
  }

  private fun removeFromOverdueListClicks_Old(): Observable<ContactPatientEvent> {
    return Observable.create { emitter ->
      emitter.setCancellable { callPatientView_Old.removeFromOverdueListClicked = null }

      callPatientView_Old.removeFromOverdueListClicked = { emitter.onNext(RemoveFromOverdueListClicked) }
    }
  }

  private fun fadeIn(viewGroup: ViewGroup) {
    val materialFade = MaterialFade().apply {
      duration = 150
    }
    TransitionManager.beginDelayedTransition(viewGroup, materialFade)
  }

  private fun sharedAxis(viewGroup: ViewGroup) {
    val sharedAxis = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
      duration = 150
    }
    TransitionManager.beginDelayedTransition(viewGroup, sharedAxis)
  }

  @Parcelize
  data class Key(val patientId: UUID) : ScreenKey() {

    override val analyticsName = "Contact Patient Bottom Sheet"

    override val type = ScreenType.Modal

    override fun instantiateFragment() = ContactPatientBottomSheet()
  }

  @Parcelize
  object RemoveOverdueAppointmentResult : Parcelable

  interface Injector {
    fun inject(target: ContactPatientBottomSheet)
  }
}
