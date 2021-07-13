package org.simple.clinic.bloodsugar.entry

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
import org.simple.clinic.ClinicApp
import org.simple.clinic.R
import org.simple.clinic.ReportAnalyticsEvents
import org.simple.clinic.bloodsugar.BloodSugarMeasurementType
import org.simple.clinic.bloodsugar.BloodSugarUnitPreference
import org.simple.clinic.bloodsugar.BloodSugarUnitPreference.Mg
import org.simple.clinic.bloodsugar.BloodSugarUnitPreference.Mmol
import org.simple.clinic.bloodsugar.Fasting
import org.simple.clinic.bloodsugar.HbA1c
import org.simple.clinic.bloodsugar.PostPrandial
import org.simple.clinic.bloodsugar.Random
import org.simple.clinic.bloodsugar.Unknown
import org.simple.clinic.bloodsugar.entry.BloodSugarEntrySheet.ScreenType.BLOOD_SUGAR_ENTRY
import org.simple.clinic.bloodsugar.entry.BloodSugarEntrySheet.ScreenType.DATE_ENTRY
import org.simple.clinic.bloodsugar.entry.OpenAs.New
import org.simple.clinic.bloodsugar.entry.OpenAs.Update
import org.simple.clinic.bloodsugar.entry.confirmremovebloodsugar.ConfirmRemoveBloodSugarDialog
import org.simple.clinic.bloodsugar.entry.confirmremovebloodsugar.ConfirmRemoveBloodSugarDialog.RemoveBloodSugarListener
import org.simple.clinic.bloodsugar.entry.di.BloodSugarEntryComponent
import org.simple.clinic.bloodsugar.unitselection.BloodSugarUnitSelectionDialog
import org.simple.clinic.databinding.SheetBloodSugarEntryBinding
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
import org.simple.clinic.widgets.UiEvent
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

class BloodSugarEntrySheet : BottomSheetActivity(), BloodSugarEntryUi, RemoveBloodSugarListener {
  enum class ScreenType {
    BLOOD_SUGAR_ENTRY,
    DATE_ENTRY
  }

  companion object {
    private const val KEY_OPEN_AS = "openAs"
    private const val EXTRA_WAS_BLOOD_SUGAR_SAVED = "wasBloodSugarSaved"

    fun intentForNewBloodSugar(
        context: Context,
        patientUuid: UUID,
        measurementType: BloodSugarMeasurementType
    ): Intent {
      val intent = Intent(context, BloodSugarEntrySheet::class.java)
      intent.putExtra(KEY_OPEN_AS, New(patientUuid, measurementType))
      return intent
    }

    fun intentForUpdateBloodSugar(
        context: Context,
        bloodSugarMeasurementUuid: UUID,
        measurementType: BloodSugarMeasurementType
    ): Intent {
      val intent = Intent(context, BloodSugarEntrySheet::class.java)
      intent.putExtra(KEY_OPEN_AS, Update(bloodSugarMeasurementUuid, measurementType))
      return intent
    }
  }

  @Inject
  @Named("exact_date")
  lateinit var dateFormatter: DateTimeFormatter

  @Inject
  lateinit var userInputDatePaddingCharacter: UserInputDatePaddingCharacter

  @Inject
  lateinit var userClock: UserClock

  @Inject
  lateinit var userTimeZone: ZoneId

  @Inject
  lateinit var bloodSugarEntryUpdate: BloodSugarEntryUpdate.Factory

  @Inject
  lateinit var bloodSugarEntryEffectHandler: BloodSugarEntryEffectHandler.Factory

  @Inject
  lateinit var locale: Locale

  @Inject
  lateinit var features: Features

  @Inject
  @DateFormatter(Day)
  lateinit var dayDateFormatter: DateTimeFormatter

  @Inject
  @DateFormatter(Month)
  lateinit var monthDateFormatter: DateTimeFormatter

  @Inject
  @DateFormatter(FullYear)
  lateinit var fullYearDateFormatter: DateTimeFormatter

  private lateinit var component: BloodSugarEntryComponent

  private val uiRenderer = BloodSugarEntryUiRenderer(this)

  private val openAs: OpenAs by lazy {
    intent.getParcelableExtra(KEY_OPEN_AS)!!
  }

  private val delegate by unsafeLazy {
    val defaultModel = BloodSugarEntryModel.create(LocalDate.now(userClock).year, openAs)

    MobiusDelegate.forActivity(
        events.ofType(),
        defaultModel,
        bloodSugarEntryUpdate.create(LocalDate.now(userTimeZone)),
        bloodSugarEntryEffectHandler.create(this).build(),
        BloodSugarEntryInit(),
        uiRenderer::render
    )
  }

  private val events: Observable<UiEvent> by unsafeLazy {
    Observable.mergeArray(
        bloodSugarTextChanges(),
        imeDoneClicks(),
        changeDateClicks(),
        backClicks(),
        hardwareBackPresses(),
        screenTypeChanges(),
        dayTextChanges(),
        monthTextChanges(),
        yearTextChanges(),
        removeClicks(),
        bloodSugarReadingUnitButtonClicks()
    )
        .compose(ReportAnalyticsEvents())
        .share()
  }

  private lateinit var binding: SheetBloodSugarEntryBinding

  private val rootLayout
    get() = binding.rootLayout

  private val bloodSugarReadingEditText
    get() = binding.bloodSugarReadingEditText

  private val dayEditText
    get() = binding.dayEditText

  private val monthEditText
    get() = binding.monthEditText

  private val yearEditText
    get() = binding.yearEditText

  private val changeDateButton
    get() = binding.changeDateButton

  private val backImageButton
    get() = binding.backImageButton

  private val viewFlipper
    get() = binding.viewFlipper

  private val removeBloodSugarButton
    get() = binding.removeBloodSugarButton

  private val bloodSugarErrorTextView
    get() = binding.bloodSugarErrorTextView

  private val dateErrorTextView
    get() = binding.dateErrorTextView

  private val enterBloodSugarTitleTextView
    get() = binding.enterBloodSugarTitleTextView

  private val editBloodSugarTitleTextView
    get() = binding.editBloodSugarTitleTextView

  private val progressLoader
    get() = binding.progressLoader

  private val bloodSugarReadingLayout
    get() = binding.bloodSugarReadingLayout

  private val bloodSugarReadingUnitButton
    get() = binding.bloodSugarReadingUnitButton

  private val bloodSugarReadingUnitLabel
    get() = binding.bloodSugarReadingUnitLabel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = SheetBloodSugarEntryBinding.inflate(layoutInflater)
    setContentView(binding.root)
    delegate.onRestoreInstanceState(savedInstanceState)
  }

  override fun onStart() {
    super.onStart()
    delegate.start()
  }

  override fun onStop() {
    super.onStop()
    delegate.stop()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    delegate.onSaveInstanceState(outState)
    super.onSaveInstanceState(outState)
  }

  override fun attachBaseContext(baseContext: Context) {
    setupDi()

    val wrappedContext = baseContext
        .wrap { InjectorProviderContextWrapper.wrap(it, component) }
        .wrap { ViewPumpContextWrapper.wrap(it) }

    super.attachBaseContext(wrappedContext)
    applyOverrideConfiguration(Configuration())
  }

  override fun applyOverrideConfiguration(overrideConfiguration: Configuration) {
    super.applyOverrideConfiguration(overrideConfiguration.withLocale(locale, features))
  }

  private fun setupDi() {
    component = ClinicApp.appComponent
        .bloodSugarEntryComponent()
        .create(activity = this)

    component.inject(this)
  }

  private fun bloodSugarTextChanges() = bloodSugarReadingEditText.textChanges()
      .map(CharSequence::toString)
      .map(::BloodSugarChanged)

  private fun imeDoneClicks(): Observable<SaveClicked> {
    return listOf(bloodSugarReadingEditText, dayEditText, monthEditText, yearEditText)
        .map { it.editorActions { actionId -> actionId == EditorInfo.IME_ACTION_DONE } }
        .toObservable()
        .flatMap { it }
        .map { SaveClicked }
  }

  private fun changeDateClicks(): Observable<UiEvent> = changeDateButton
      .clicks()
      .map { ChangeDateClicked }

  private fun backClicks(): Observable<UiEvent> = backImageButton
      .clicks()
      .map { ShowBloodSugarEntryClicked }

  private fun bloodSugarReadingUnitButtonClicks() = bloodSugarReadingUnitButton
      .clicks()
      .map { BloodSugarReadingUnitButtonClicked }

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
              R.id.bloodsugarentry_flipper_blood_sugar_entry -> BLOOD_SUGAR_ENTRY
              R.id.bloodsugarentry_flipper_date_entry -> DATE_ENTRY
              else -> throw AssertionError()
            })
          }

  private fun dayTextChanges() = dayEditText
      .textChanges()
      .map(CharSequence::toString)
      .map(::DayChanged)

  private fun monthTextChanges() = monthEditText
      .textChanges()
      .map(CharSequence::toString)
      .map(::MonthChanged)

  private fun yearTextChanges() = yearEditText
      .textChanges()
      .map(CharSequence::toString)
      .map(::YearChanged)

  private fun removeClicks() = removeBloodSugarButton
      .clicks()
      .map { RemoveBloodSugarClicked }

  override fun setBloodSugarSavedResultAndFinish() {
    val intent = Intent()
    intent.putExtra(EXTRA_WAS_BLOOD_SUGAR_SAVED, true)
    setResult(Activity.RESULT_OK, intent)
    finish()
  }

  override fun hideBloodSugarErrorMessage() {
    bloodSugarErrorTextView.visibleOrGone(false)
  }

  override fun showBloodSugarEmptyError() {
    showBloodSugarErrorMessage(getString(R.string.bloodsugarentry_error_empty))
  }

  override fun showBloodSugarHighError(
      measurementType: BloodSugarMeasurementType,
      unitPreference: BloodSugarUnitPreference
  ) {
    when (unitPreference) {
      Mg -> showBloodSugarHighErrorForMg(measurementType)
      Mmol -> showBloodSugarHighErrorForMmol(measurementType)
    }
  }

  private fun showBloodSugarHighErrorForMg(measurementType: BloodSugarMeasurementType) {
    when (measurementType) {
      Random, PostPrandial, Fasting -> showBloodSugarErrorMessage(getString(R.string.bloodsugarentry_error_higher_limit))
      HbA1c -> showBloodSugarErrorMessage(getString(R.string.bloodsugarentry_error_higher_limit_hba1c))
    }
  }

  private fun showBloodSugarHighErrorForMmol(measurementType: BloodSugarMeasurementType) {
    when (measurementType) {
      Random, PostPrandial, Fasting -> showBloodSugarErrorMessage(getString(R.string.bloodsugarentry_error_higher_limit_mmol))
    }
  }

  override fun showBloodSugarLowError(
      measurementType: BloodSugarMeasurementType,
      unitPreference: BloodSugarUnitPreference
  ) {
    when (unitPreference) {
      Mg -> showBloodSugarLowErrorForMg(measurementType)
      Mmol -> showBloodSugarLowErrorForMmol(measurementType)
    }
  }

  private fun showBloodSugarLowErrorForMg(measurementType: BloodSugarMeasurementType) {
    when (measurementType) {
      Random, PostPrandial, Fasting -> showBloodSugarErrorMessage(getString(R.string.bloodsugarentry_error_lower_limit))
      HbA1c -> showBloodSugarErrorMessage(getString(R.string.bloodsugarentry_error_lower_limit_hba1c))
    }
  }

  private fun showBloodSugarLowErrorForMmol(measurementType: BloodSugarMeasurementType) {
    when (measurementType) {
      Random, PostPrandial, Fasting -> showBloodSugarErrorMessage(getString(R.string.bloodsugarentry_error_lower_limit_mmol))
    }
  }

  override fun showBloodSugarEntryScreen() {
    viewFlipper.inAnimation = AnimationUtils
        .loadAnimation(this, R.anim.measurementinput_reading_entry_from_left)
        .apply { interpolator = FastOutSlowInInterpolator() }

    viewFlipper.outAnimation = AnimationUtils
        .loadAnimation(this, R.anim.measurementinput_date_exit_to_right)
        .apply { interpolator = FastOutSlowInInterpolator() }

    viewFlipper.displayedChildResId = R.id.bloodsugarentry_flipper_blood_sugar_entry
  }

  override fun showDateEntryScreen() {
    viewFlipper.inAnimation = AnimationUtils
        .loadAnimation(this, R.anim.measurementinput_date_entry_from_right)
        .apply { interpolator = FastOutSlowInInterpolator() }

    viewFlipper.outAnimation = AnimationUtils
        .loadAnimation(this, R.anim.measurementinput_reading_exit_to_left)
        .apply { interpolator = FastOutSlowInInterpolator() }

    viewFlipper.displayedChildResId = R.id.bloodsugarentry_flipper_date_entry
    yearEditText.requestFocus()
  }

  override fun showInvalidDateError() {
    showDateErrorMessage(getString(R.string.bloodsugarentry_error_date_invalid_pattern))
  }

  override fun showDateIsInFutureError() {
    showDateErrorMessage(getString(R.string.bloodsugarentry_error_date_is_in_future))
  }

  override fun hideDateErrorMessage() {
    dateErrorTextView.visibleOrGone(false)
  }

  override fun setDateOnInputFields(date: LocalDate) {
    dayEditText.setTextAndCursor(dayDateFormatter.format(date))
    monthEditText.setTextAndCursor(monthDateFormatter.format(date))
    yearEditText.setTextAndCursor(fullYearDateFormatter.format(date))
  }

  override fun showDateOnDateButton(date: LocalDate) {
    changeDateButton.text = dateFormatter.format(date)
  }

  override fun showEntryTitle(measurementType: BloodSugarMeasurementType) {
    enterBloodSugarTitleTextView.visibility = View.VISIBLE
    editBloodSugarTitleTextView.visibility = View.GONE

    val title = when (measurementType) {
      Random -> getString(R.string.bloodsugarentry_random_title)
      PostPrandial -> getString(R.string.bloodsugarentry_post_prandial_title)
      Fasting -> getString(R.string.bloodsugarentry_fasting_title)
      HbA1c -> getString(R.string.bloodsugarentry_hba1c_title)
      is Unknown -> measurementType.actualValue
    }
    enterBloodSugarTitleTextView.text = title
  }

  override fun showEditTitle(measurementType: BloodSugarMeasurementType) {
    enterBloodSugarTitleTextView.visibility = View.GONE
    editBloodSugarTitleTextView.visibility = View.VISIBLE

    val title = when (measurementType) {
      Random -> getString(R.string.bloodsugarentry_edit_random_title)
      PostPrandial -> getString(R.string.bloodsugarentry_edit_post_prandial_title)
      Fasting -> getString(R.string.bloodsugarentry_edit_fasting_title)
      HbA1c -> getString(R.string.bloodsugarentry_edit_hba1c_title)
      is Unknown -> measurementType.actualValue
    }
    editBloodSugarTitleTextView.text = title
  }

  override fun showProgress() {
    progressLoader.visibleOrGone(isVisible = true)
    bloodSugarReadingLayout.visibleOrGone(isVisible = false)
    changeDateButton.visibleOrGone(isVisible = false)
  }

  override fun hideProgress() {
    progressLoader.visibleOrGone(isVisible = false)
    bloodSugarReadingLayout.visibleOrGone(isVisible = true)
    changeDateButton.visibleOrGone(isVisible = true)
  }

  override fun setBloodSugarUnitPreferenceLabelToMmol() {
    bloodSugarReadingUnitButton.text = getString(R.string.bloodsugarentry_mmol_l)
  }

  override fun setBloodSugarUnitPreferenceLabelToMg() {
    bloodSugarReadingUnitButton.text = getString(R.string.bloodsugarentry_mg_dl)
  }

  override fun showBloodSugarUnitPreferenceButton() {
    bloodSugarReadingUnitButton.visibility = View.VISIBLE
  }

  override fun hideBloodSugarUnitPreferenceButton() {
    bloodSugarReadingUnitButton.visibility = View.GONE
  }

  override fun showBloodSugarUnitPreferenceLabel() {
    bloodSugarReadingUnitLabel.visibility = View.VISIBLE
  }

  override fun hideBloodSugarUnitPreferenceLabel() {
    bloodSugarReadingUnitLabel.visibility = View.GONE
  }

  override fun decimalOrNumericBloodSugarInputType() {
    bloodSugarReadingEditText.inputType = EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_FLAG_DECIMAL
  }

  override fun numericBloodSugarInputType() {
    bloodSugarReadingEditText.inputType = EditorInfo.TYPE_CLASS_NUMBER
  }

  override fun setLabelForHbA1c() {
    bloodSugarReadingUnitLabel.text = getString(R.string.bloodsugarentry_percentage)
  }

  override fun setLabelForUnknown() {
    bloodSugarReadingUnitLabel.text = getString(R.string.bloodsugarentry_mg_dl)
  }

  override fun showBloodSugarUnitSelectionDialog(bloodSugarUnitPreference: BloodSugarUnitPreference) {
    BloodSugarUnitSelectionDialog.show(supportFragmentManager, bloodSugarUnitPreference)
  }

  override fun showRemoveButton() {
    removeBloodSugarButton.visibility = View.VISIBLE
  }

  override fun hideRemoveButton() {
    removeBloodSugarButton.visibility = View.GONE
  }

  override fun setBloodSugarReading(bloodSugarReading: String) {
    bloodSugarReadingEditText.setTextAndCursor(bloodSugarReading)
  }

  override fun dismiss() {
    finish()
  }

  override fun onBackgroundClick() {
    if (bloodSugarReadingEditText.text.isNullOrBlank()) {
      super.onBackgroundClick()
    }
  }

  override fun showConfirmRemoveBloodSugarDialog(bloodSugarMeasurementUuid: UUID) {
    ConfirmRemoveBloodSugarDialog.show(bloodSugarMeasurementUuid, supportFragmentManager)
  }

  override fun onBloodSugarRemoved() {
    dismiss()
  }

  private fun showBloodSugarErrorMessage(message: String) {
    with(bloodSugarErrorTextView) {
      text = message
      visibility = View.VISIBLE
    }
  }

  private fun showDateErrorMessage(message: String) {
    with(dateErrorTextView) {
      text = message
      visibility = View.VISIBLE
    }
  }

  private fun getPaddedString(value: String): String =
      value.padStart(length = 2, padChar = userInputDatePaddingCharacter.value)
}
