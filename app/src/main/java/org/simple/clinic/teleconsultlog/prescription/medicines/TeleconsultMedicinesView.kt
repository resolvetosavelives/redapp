package org.simple.clinic.teleconsultlog.prescription.medicines

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.Observable
import io.reactivex.rxkotlin.ofType
import kotlinx.android.synthetic.main.view_teleconsult_medicines.view.*
import org.simple.clinic.R
import org.simple.clinic.ReportAnalyticsEvents
import org.simple.clinic.di.injector
import org.simple.clinic.drugs.PrescribedDrug
import org.simple.clinic.drugs.selection.PrescribedDrugsScreenKey
import org.simple.clinic.mobius.MobiusDelegate
import org.simple.clinic.router.screen.ActivityResult
import org.simple.clinic.router.screen.ScreenRouter
import org.simple.clinic.teleconsultlog.drugduration.DrugDuration
import org.simple.clinic.teleconsultlog.drugduration.DrugDurationSheet
import org.simple.clinic.teleconsultlog.medicinefrequency.MedicineFrequencySheet
import org.simple.clinic.teleconsultlog.medicinefrequency.MedicineFrequencySheetExtra
import org.simple.clinic.teleconsultlog.prescription.TeleconsultPrescriptionScreenKey
import org.simple.clinic.util.extractSuccessful
import org.simple.clinic.util.unsafeLazy
import org.simple.clinic.widgets.DividerItemDecorator
import org.simple.clinic.widgets.ItemAdapter
import org.simple.clinic.widgets.UiEvent
import java.util.UUID
import javax.inject.Inject

class TeleconsultMedicinesView(
    context: Context,
    attrs: AttributeSet?
) : ConstraintLayout(context, attrs), TeleconsultMedicinesUi, TeleconsultMedicinesUiActions {

  @Inject
  lateinit var effectHandlerFactory: TeleconsultMedicinesEffectHandler.Factory

  @Inject
  lateinit var screenRouter: ScreenRouter

  @Inject
  lateinit var activity: AppCompatActivity

  @Inject
  lateinit var teleconsultMedicinesConfig: TeleconsultMedicinesConfig

  companion object {
    private const val DRUG_FREQUENCY_SHEET = 1
    private const val DRUG_DURATION_SHEET = 2
  }

  init {
    LayoutInflater.from(context).inflate(R.layout.view_teleconsult_medicines, this)
  }

  private val screenKey by unsafeLazy {
    screenRouter.key<TeleconsultPrescriptionScreenKey>(this)
  }

  private val events by unsafeLazy {
    Observable
        .mergeArray(
            editClicks(),
            drugDurationChanges(),
            drugFrequencyChanges(),
            drugDurationClicks(),
            drugFrequencyClicks()
        )
        .compose(ReportAnalyticsEvents())
  }

  private val delegate by unsafeLazy {
    val uiRenderer = TeleconsultMedicinesUiRenderer(this)

    MobiusDelegate.forView(
        events = events.ofType(),
        defaultModel = TeleconsultMedicinesModel.create(patientUuid = screenKey.patientUuid),
        init = TeleconsultMedicinesInit(),
        update = TeleconsultMedicinesUpdate(),
        effectHandler = effectHandlerFactory.create(this).build(),
        modelUpdateListener = uiRenderer::render
    )
  }

  private val teleconsultMedicinesAdapter = ItemAdapter(TeleconsultMedicineDiffCallback())

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    delegate.start()
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    delegate.stop()
  }

  override fun onSaveInstanceState(): Parcelable? {
    return delegate.onSaveInstanceState(super.onSaveInstanceState())
  }

  override fun onRestoreInstanceState(state: Parcelable?) {
    super.onRestoreInstanceState(delegate.onRestoreInstanceState(state))
  }

  override fun onFinishInflate() {
    super.onFinishInflate()
    if (isInEditMode) return
    context.injector<Injector>().inject(this)

    medicinesRecyclerView.adapter = teleconsultMedicinesAdapter
    medicinesRecyclerView.addItemDecoration(DividerItemDecorator(context, 0, 0))
  }

  override fun renderMedicines(medicines: List<PrescribedDrug>) {
    emptyMedicinesTextView.visibility = GONE
    medicinesRecyclerView.visibility = VISIBLE

    teleconsultMedicinesAdapter.submitList(TeleconsultMedicineItem.from(
        medicines = medicines,
        defaultDuration = teleconsultMedicinesConfig.defaultDuration,
        defaultFrequency = teleconsultMedicinesConfig.defaultFrequency
    ))
  }

  override fun showNoMedicines() {
    emptyMedicinesTextView.visibility = VISIBLE
    medicinesRecyclerView.visibility = GONE
  }

  override fun showAddButton() {
    medicinesEditButton.text = context.getString(R.string.view_teleconsult_medicines_add)
  }

  override fun showEditButton() {
    medicinesEditButton.text = context.getString(R.string.view_teleconsult_medicines_edit)
  }

  override fun openEditMedicines(patientUuid: UUID) {
    screenRouter.push(PrescribedDrugsScreenKey(patientUuid))
  }

  override fun openDrugDurationSheet(prescription: PrescribedDrug) {
    val durationInDays = prescription.durationInDays?.toString() ?: teleconsultMedicinesConfig.defaultDuration.toDays().toString()
    val intent = DrugDurationSheet.intent(
        context = context,
        drugDuration = DrugDuration(
            uuid = prescription.uuid,
            name = prescription.name,
            dosage = prescription.dosage,
            duration = durationInDays
        )
    )
    activity.startActivityForResult(intent, DRUG_DURATION_SHEET)
  }

  override fun openDrugFrequencySheet(prescription: PrescribedDrug) {
    val frequency = prescription.frequency ?: teleconsultMedicinesConfig.defaultFrequency
    val intent = MedicineFrequencySheet.intent(
        context = context,
        medicineFrequencySheetExtra = MedicineFrequencySheetExtra(
            uuid = prescription.uuid,
            name = prescription.name,
            dosage = prescription.dosage,
            medicineFrequency = frequency
        )
    )
    activity.startActivityForResult(intent, DRUG_FREQUENCY_SHEET)
  }

  fun showMedicinesRequiredError() {
    medicinesRequiredErrorTextView.visibility = View.VISIBLE
  }

  private fun editClicks(): Observable<UiEvent> {
    return medicinesEditButton
        .clicks()
        .map { EditMedicinesClicked }
  }

  private fun drugDurationChanges(): Observable<UiEvent> {
    return screenRouter.streamScreenResults()
        .ofType<ActivityResult>()
        .extractSuccessful(DRUG_DURATION_SHEET) { intent ->
          DrugDurationSheet.readSavedDrugDuration(intent)
        }
        .map { (uuid, duration) -> DrugDurationChanged(uuid, duration) }
  }

  private fun drugFrequencyChanges(): Observable<UiEvent> {
    return screenRouter.streamScreenResults()
        .ofType<ActivityResult>()
        .extractSuccessful(DRUG_FREQUENCY_SHEET) { intent ->
          MedicineFrequencySheet.readSavedDrugFrequency(intent)
        }
        .map { (uuid, frequency) -> DrugFrequencyChanged(uuid, frequency) }
  }

  private fun drugDurationClicks(): Observable<UiEvent> {
    return teleconsultMedicinesAdapter.itemEvents
        .ofType<DrugDurationButtonClicked>()
        .map {
          DrugDurationClicked(it.prescribedDrug)
        }
  }

  private fun drugFrequencyClicks(): Observable<UiEvent> {
    return teleconsultMedicinesAdapter.itemEvents
        .ofType<DrugFrequencyButtonClicked>()
        .map {
          DrugFrequencyClicked(it.prescribedDrug)
        }
  }

  interface Injector {
    fun inject(target: TeleconsultMedicinesView)
  }
}
