package org.simple.clinic.bp.assignbppassport

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.jakewharton.rxbinding3.view.clicks
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import io.reactivex.Observable
import kotlinx.android.parcel.Parcelize
import org.simple.clinic.ClinicApp
import org.simple.clinic.R
import org.simple.clinic.databinding.SheetBpPassportBinding
import org.simple.clinic.di.InjectorProviderContextWrapper
import org.simple.clinic.mobius.MobiusDelegate
import org.simple.clinic.navigation.v2.Router
import org.simple.clinic.navigation.v2.ScreenKey
import org.simple.clinic.navigation.v2.Succeeded
import org.simple.clinic.navigation.v2.fragments.BaseBottomSheet
import org.simple.clinic.patient.businessid.Identifier
import org.simple.clinic.util.unsafeLazy
import org.simple.clinic.util.wrap
import javax.inject.Inject

class BpPassportSheet :
    BaseBottomSheet<
        BpPassportSheet.Key,
        SheetBpPassportBinding,
        BpPassportModel,
        BpPassportEvent,
        BpPassportEffect>(), BpPassportUiActions {

  companion object {
    private const val KEY_BP_PASSPORT_NUMBER = "bpPassportNumber"
    private const val BP_PASSPORT_RESULT = "bpPassportResult"

    fun intent(
        context: Context,
        bpPassportNumber: Identifier
    ): Intent {
      val intent = Intent(context, BpPassportSheet::class.java)
      intent.putExtra(KEY_BP_PASSPORT_NUMBER, bpPassportNumber)
      return intent
    }

    fun blankBpPassportResult(data: Intent): BlankBpPassportResult? {
      return data.getParcelableExtra(BP_PASSPORT_RESULT)
    }
  }

  @Inject
  lateinit var router: Router

  @Inject
  lateinit var effectHandlerFactory: BpPassportEffectHandler.Factory

  private lateinit var component: BpPassportSheetComponent

  private val registerNewPatientButton
    get() = binding.registerNewPatientButton

  private val addToExistingPatientButton
    get() = binding.addToExistingPatientButton

  private val bpPassportNumberTextview
    get() = binding.bpPassportNumberTextview

  private val events: Observable<BpPassportEvent> by unsafeLazy {
    Observable
        .merge(
            registerNewPatientClicks(),
            addToExistingPatientClicks()
        )
  }

  private val delegate by unsafeLazy {

    MobiusDelegate.forActivity(
        events = events,
        defaultModel = BpPassportModel.create(identifier = bpPassportIdentifier),
        update = BpPassportUpdate(),
        effectHandler = effectHandlerFactory.create(this).build(),
    )
  }

  private val bpPassportIdentifier: Identifier by lazy {
    screenKey.identifier
  }

  override fun defaultModel() = BpPassportModel.create(bpPassportIdentifier)

  override fun bindView(inflater: LayoutInflater, container: ViewGroup?): SheetBpPassportBinding {
    return SheetBpPassportBinding.inflate(layoutInflater, container, false)
  }

  override fun events(): Observable<BpPassportEvent> {
    return Observable
        .merge(
            registerNewPatientClicks(),
            addToExistingPatientClicks()
        )
  }

  override fun createUpdate() = BpPassportUpdate()

  override fun createEffectHandler() = effectHandlerFactory.create(this).build()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = SheetBpPassportBinding.inflate(layoutInflater)
    setContentView(binding.root)

    bpPassportNumberTextview.text = getString(R.string.sheet_bp_passport_number, bpPassportIdentifier.displayValue())
    delegate.onRestoreInstanceState(savedInstanceState)
  }

  override fun sendBpPassportResult(blankBpPassportResult: BlankBpPassportResult) {
    router.popWithResult(Succeeded(blankBpPassportResult))
  }

  private fun addToExistingPatientClicks(): Observable<BpPassportEvent> {
    return addToExistingPatientButton
        .clicks()
        .map { AddToExistingPatientClicked }
  }

  private fun registerNewPatientClicks(): Observable<BpPassportEvent> {
    return registerNewPatientButton
        .clicks()
        .map { RegisterNewPatientClicked }
  }

  private fun setUpDiGraph() {
    component = ClinicApp.appComponent
        .bpPassportSheetComponent()
        .create(activity = this)

    component.inject(this)
  }

  override fun attachBaseContext(newBase: Context) {
    setUpDiGraph()

    val wrappedContext = newBase
        .wrap { InjectorProviderContextWrapper.wrap(it, component) }
        .wrap { ViewPumpContextWrapper.wrap(it) }

    super.attachBaseContext(wrappedContext)
    applyOverrideConfiguration(Configuration())
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

  @Parcelize
  data class Key(
      val identifier: Identifier
  ) : ScreenKey() {

    override val analyticsName = "Blank BP passport sheet"

    override fun instantiateFragment(): Fragment {

    }

    override val type = ScreenType.Modal
  }
}
