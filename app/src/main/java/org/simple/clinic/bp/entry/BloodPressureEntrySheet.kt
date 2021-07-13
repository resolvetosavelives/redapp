package org.simple.clinic.bp.entry

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import io.reactivex.Observable
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.toObservable
import io.reactivex.subjects.PublishSubject
import org.simple.clinic.ClinicApp
import org.simple.clinic.R
import org.simple.clinic.ReportAnalyticsEvents
import org.simple.clinic.bp.entry.BloodPressureEntrySheet.ScreenType.BP_ENTRY
import org.simple.clinic.bp.entry.BloodPressureEntrySheet.ScreenType.DATE_ENTRY
import org.simple.clinic.bp.entry.OpenAs.New
import org.simple.clinic.bp.entry.OpenAs.Update
import org.simple.clinic.bp.entry.confirmremovebloodpressure.ConfirmRemoveBloodPressureDialog
import org.simple.clinic.bp.entry.confirmremovebloodpressure.ConfirmRemoveBloodPressureDialog.RemoveBloodPressureListener
import org.simple.clinic.bp.entry.di.BloodPressureEntryComponent
import org.simple.clinic.databinding.SheetBloodPressureEntryBinding
import org.simple.clinic.di.DateFormatter
import org.simple.clinic.di.DateFormatter.Type.Day
import org.simple.clinic.di.DateFormatter.Type.FullYear
import org.simple.clinic.di.DateFormatter.Type.Month
import org.simple.clinic.di.InjectorProviderContextWrapper
import org.simple.clinic.feature.Features
import org.simple.clinic.mobius.MobiusDelegate
import org.simple.clinic.util.UserClock
import org.simple.clinic.util.UserInputDatePaddingCharacter
import org.simple.clinic.util.unsafeLazy
import org.simple.clinic.util.withLocale
import org.simple.clinic.util.wrap
import org.simple.clinic.widgets.BottomSheetActivity
import org.simple.clinic.widgets.ScreenDestroyed
import org.simple.clinic.widgets.UiEvent
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator
import org.simple.clinic.widgets.displayedChildResId
import org.simple.clinic.widgets.setTextAndCursor
import org.simple.clinic.widgets.visibleOrGone
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

class BloodPressureEntrySheet : BottomSheetActivity(), BloodPressureEntryUi, RemoveBloodPressureListener {

  @Inject
  @Named("full_date")
  lateinit var dateFormatter: DateTimeFormatter

  @Inject
  lateinit var locale: Locale

  @Inject
  lateinit var features: Features

  private val screenDestroys = PublishSubject.create<ScreenDestroyed>()

  enum class ScreenType {
    BP_ENTRY,
    DATE_ENTRY
  }

  companion object {
    private const val KEY_OPEN_AS = "openAs"
    private const val EXTRA_WAS_BP_SAVED = "wasBpSaved"

    fun intentForNewBp(context: Context, patientUuid: UUID): Intent {
      return Intent(context, BloodPressureEntrySheet::class.java)
          .putExtra(KEY_OPEN_AS, New(patientUuid))
    }

    fun intentForUpdateBp(context: Context, bloodPressureMeasurementUuid: UUID): Intent {
      return Intent(context, BloodPressureEntrySheet::class.java)
          .putExtra(KEY_OPEN_AS, Update(bloodPressureMeasurementUuid))
    }

    fun wasBloodPressureSaved(data: Intent): Boolean {
      return data.getBooleanExtra(EXTRA_WAS_BP_SAVED, false)
    }
  }

  @Inject
  lateinit var userInputDatePaddingCharacter: UserInputDatePaddingCharacter

  @Inject
  lateinit var userClock: UserClock

  @Inject
  lateinit var userTimeZone: ZoneId

  @Inject
  lateinit var dateValidator: UserInputDateValidator

  @Inject
  lateinit var effectHandlerFactory: BloodPressureEntryEffectHandler.Factory

  @Inject
  @DateFormatter(Day)
  lateinit var dayDateFormatter: DateTimeFormatter

  @Inject
  @DateFormatter(Month)
  lateinit var monthDateFormatter: DateTimeFormatter

  @Inject
  @DateFormatter(FullYear)
  lateinit var fullYearDateFormatter: DateTimeFormatter

  private lateinit var component: BloodPressureEntryComponent

  private val uiRenderer = BloodPressureEntryUiRenderer(this)

  private val delegate by unsafeLazy {
    val openAs = intent.extras!!.getParcelable<OpenAs>(KEY_OPEN_AS)!!
    val defaultModel = BloodPressureEntryModel.create(openAs, LocalDate.now(userClock).year)

    MobiusDelegate.forActivity(
        events.ofType(),
        defaultModel,
        BloodPressureEntryUpdate(dateValidator, LocalDate.now(userTimeZone), userInputDatePaddingCharacter),
        effectHandlerFactory.create(this).build(),
        BloodPressureEntryInit(),
        uiRenderer::render
    )
  }

  private val events: Observable<UiEvent> by unsafeLazy {
    Observable.mergeArray(
        systolicTextChanges(),
        diastolicTextChanges(),
        imeDoneClicks(),
        diastolicBackspaceClicks(),
        removeClicks(),
        changeDateButtonClicks(),
        backClicks(),
        hardwareBackPresses(),
        screenTypeChanges(),
        dayTextChanges(),
        monthTextChanges(),
        yearTextChanges()
    )
        .compose(ReportAnalyticsEvents())
        .share()
  }

  private lateinit var binding: SheetBloodPressureEntryBinding

  private val systolicEditText
    get() = binding.systolicEditText

  private val diastolicEditText
    get() = binding.diastolicEditText

  private val dayEditText
    get() = binding.dayEditText

  private val monthEditText
    get() = binding.monthEditText

  private val yearEditText
    get() = binding.yearEditText

  private val removeBloodPressureButton
    get() = binding.removeBloodPressureButton

  private val bpDateTextView
    get() = binding.bpDateTextView

  private val changeDateButton
    get() = binding.changeDateButton

  private val backImageButton
    get() = binding.backImageButton

  private val rootLayout
    get() = binding.rootLayout

  private val viewFlipper
    get() = binding.viewFlipper

  private val bpErrorTextView
    get() = binding.bpErrorTextView

  private val enterBloodPressureTitleTextView
    get() = binding.enterBloodPressureTitleTextView

  private val editBloodPressureTitleTextView
    get() = binding.editBloodPressureTitleTextView

  private val dateErrorTextView
    get() = binding.dateErrorTextView

  private val progressLoader
    get() = binding.progressLoader

  private val bloodPressureEntryLayout
    get() = binding.bloodPressureEntryLayout

  @SuppressLint("CheckResult")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = SheetBloodPressureEntryBinding.inflate(layoutInflater)
    setContentView(binding.root)

    delegate.onRestoreInstanceState(savedInstanceState)
  }

  override fun attachBaseContext(baseContext: Context) {
    setupDiGraph()

    val wrappedContext = baseContext
        .wrap { InjectorProviderContextWrapper.wrap(it, component) }
        .wrap { ViewPumpContextWrapper.wrap(it) }

    super.attachBaseContext(wrappedContext)
    applyOverrideConfiguration(Configuration())
  }

  override fun applyOverrideConfiguration(overrideConfiguration: Configuration) {
    super.applyOverrideConfiguration(overrideConfiguration.withLocale(locale, features))
  }

  private fun setupDiGraph() {
    component = ClinicApp.appComponent
        .bloodPressureEntryComponent()
        .create(activity = this)

    component.inject(this)
  }

  override fun onStart() {
    super.onStart()
    delegate.start()
  }

  override fun onStop() {
    delegate.stop()
    super.onStop()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    delegate.onSaveInstanceState(outState)
    super.onSaveInstanceState(outState)
  }

  override fun onDestroy() {
    screenDestroys.onNext(ScreenDestroyed())
    super.onDestroy()
  }

  override fun onBackgroundClick() {
    if (systolicEditText.text.isNullOrBlank() && diastolicEditText.text.isNullOrBlank()) {
      super.onBackgroundClick()
    }
  }

  private fun systolicTextChanges() = systolicEditText
      .textChanges()
      .map(CharSequence::toString)
      .map(::SystolicChanged)

  private fun diastolicTextChanges() = diastolicEditText
      .textChanges()
      .map(CharSequence::toString)
      .map(::DiastolicChanged)

  private fun imeDoneClicks(): Observable<SaveClicked> {
    return listOf(systolicEditText, diastolicEditText, dayEditText, monthEditText, yearEditText)
        .map { it.editorActions { actionId -> actionId == EditorInfo.IME_ACTION_DONE } }
        .toObservable()
        .flatMap { it }
        .map { SaveClicked }
  }

  private fun diastolicBackspaceClicks(): Observable<UiEvent> {
    return diastolicEditText
        .backspaceClicks
        .map { DiastolicBackspaceClicked }
  }

  private fun removeClicks(): Observable<UiEvent> =
      removeBloodPressureButton
          .clicks()
          .map { RemoveBloodPressureClicked }

  private fun changeDateButtonClicks(): Observable<UiEvent> =
      changeDateButton
          .clicks()
          .map { ChangeDateClicked }

  private fun backClicks(): Observable<UiEvent> =
      backImageButton
          .clicks()
          .map { ShowBpClicked }

  private fun hardwareBackPresses(): Observable<UiEvent> {
    return Observable.create { emitter ->
      val interceptor = {
        emitter.onNext(BackPressed)
      }
      emitter.setCancellable { rootLayout.backKeyPressInterceptor = null }
      rootLayout.backKeyPressInterceptor = interceptor
    }
  }

  private fun screenTypeChanges(): Observable<UiEvent> =
      viewFlipper
          .displayedChildChanges
          .map {
            ScreenChanged(when (viewFlipper.displayedChildResId) {
              R.id.bloodpressureentry_flipper_bp_entry -> BP_ENTRY
              R.id.bloodpressureentry_flipper_date_entry -> DATE_ENTRY
              else -> throw AssertionError()
            })
          }

  private fun dayTextChanges() =
      dayEditText
          .textChanges()
          .map(CharSequence::toString)
          .map(::DayChanged)

  private fun monthTextChanges() =
      monthEditText
          .textChanges()
          .map(CharSequence::toString)
          .map(::MonthChanged)

  private fun yearTextChanges() =
      yearEditText
          .textChanges()
          .map(CharSequence::toString)
          .map(::YearChanged)

  override fun changeFocusToDiastolic() {
    diastolicEditText.requestFocus()
  }

  override fun changeFocusToSystolic() {
    systolicEditText.requestFocus()
  }

  override fun setBpSavedResultAndFinish() {
    markBpAsSavedAndFinish()
  }

  override fun hideBpErrorMessage() {
    bpErrorTextView.visibility = View.GONE
  }

  override fun showSystolicLessThanDiastolicError() {
    showBpErrorMessage(getString(R.string.bloodpressureentry_error_systolic_more))
  }

  override fun showSystolicLowError() {
    showBpErrorMessage(getString(R.string.bloodpressureentry_error_systolic_70))
  }

  override fun showSystolicHighError() {
    showBpErrorMessage(getString(R.string.bloodpressureentry_error_systolic_300))
  }

  override fun showDiastolicLowError() {
    showBpErrorMessage(getString(R.string.bloodpressureentry_error_diastolic_40))
  }

  override fun showDiastolicHighError() {
    showBpErrorMessage(getString(R.string.bloodpressureentry_error_diastolic_180))
  }

  override fun showSystolicEmptyError() {
    showBpErrorMessage(getString(R.string.bloodpressureentry_error_systolic_empty))
  }

  override fun showDiastolicEmptyError() {
    showBpErrorMessage(getString(R.string.bloodpressureentry_error_diastolic_empty))
  }

  override fun setSystolic(systolic: String) {
    systolicEditText.setTextAndCursor(systolic)
  }

  override fun setDiastolic(diastolic: String) {
    diastolicEditText.setTextAndCursor(diastolic)
  }

  override fun showRemoveBpButton() {
    removeBloodPressureButton.visibility = View.VISIBLE
    removeBloodPressureButton.isEnabled = true
  }

  override fun hideRemoveBpButton() {
    removeBloodPressureButton.visibility = View.GONE
    removeBloodPressureButton.isEnabled = false
  }

  override fun showEnterNewBloodPressureTitle() {
    enterBloodPressureTitleTextView.visibility = View.VISIBLE
  }

  override fun showEditBloodPressureTitle() {
    editBloodPressureTitleTextView.visibility = View.VISIBLE
  }

  override fun showConfirmRemoveBloodPressureDialog(uuid: UUID) {
    ConfirmRemoveBloodPressureDialog.show(uuid, supportFragmentManager)
  }

  override fun showBpEntryScreen() {
    viewFlipper.inAnimation = AnimationUtils
        .loadAnimation(this, R.anim.measurementinput_reading_entry_from_left)
        .apply { interpolator = FastOutSlowInInterpolator() }

    viewFlipper.outAnimation = AnimationUtils
        .loadAnimation(this, R.anim.measurementinput_date_exit_to_right)
        .apply { interpolator = FastOutSlowInInterpolator() }

    viewFlipper.displayedChildResId = R.id.bloodpressureentry_flipper_bp_entry
  }

  override fun showDateEntryScreen() {
    viewFlipper.inAnimation = AnimationUtils
        .loadAnimation(this, R.anim.measurementinput_date_entry_from_right)
        .apply { interpolator = FastOutSlowInInterpolator() }

    viewFlipper.outAnimation = AnimationUtils
        .loadAnimation(this, R.anim.measurementinput_reading_exit_to_left)
        .apply { interpolator = FastOutSlowInInterpolator() }

    viewFlipper.displayedChildResId = R.id.bloodpressureentry_flipper_date_entry
    yearEditText.requestFocus()
  }

  override fun showInvalidDateError() {
    dateErrorTextView.setText(R.string.bloodpressureentry_error_date_invalid_pattern)
    dateErrorTextView.visibility = View.VISIBLE
  }

  override fun showDateIsInFutureError() {
    dateErrorTextView.setText(R.string.bloodpressureentry_error_date_is_in_future)
    dateErrorTextView.visibility = View.VISIBLE
  }

  override fun hideDateErrorMessage() {
    dateErrorTextView.visibility = View.GONE
  }

  override fun setDateOnInputFields(date: LocalDate) {
    dayEditText.setTextAndCursor(dayDateFormatter.format(date))
    monthEditText.setTextAndCursor(monthDateFormatter.format(date))
    yearEditText.setTextAndCursor(fullYearDateFormatter.format(date))
  }

  override fun showBpDate(date: LocalDate) {
    bpDateTextView.text = dateFormatter.format(date)
  }

  override fun dismiss() {
    finish()
  }

  override fun showProgress() {
    progressLoader.visibleOrGone(isVisible = true)
    bloodPressureEntryLayout.visibleOrGone(isVisible = false)
    changeDateButton.visibleOrGone(isVisible = false)
    removeBloodPressureButton.visibleOrGone(isVisible = false)
  }

  override fun hideProgress() {
    progressLoader.visibleOrGone(isVisible = false)
    bloodPressureEntryLayout.visibleOrGone(isVisible = true)
    changeDateButton.visibleOrGone(isVisible = true)
    if (removeBloodPressureButton.isEnabled)
      removeBloodPressureButton.visibleOrGone(isVisible = true)
  }

  override fun onBloodPressureRemoved() {
    markBpAsSavedAndFinish()
  }

  private fun markBpAsSavedAndFinish() {
    val intent = Intent()
    intent.putExtra(EXTRA_WAS_BP_SAVED, true)
    setResult(Activity.RESULT_OK, intent)
    finish()
  }

  private fun showBpErrorMessage(message: String) {
    with(bpErrorTextView) {
      text = message
      visibility = View.VISIBLE
    }
  }

  private fun getPaddedString(value: String): String =
      value.padStart(length = 2, padChar = userInputDatePaddingCharacter.value)
}
