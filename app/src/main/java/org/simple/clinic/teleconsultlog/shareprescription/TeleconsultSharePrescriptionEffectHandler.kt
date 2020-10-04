package org.simple.clinic.teleconsultlog.shareprescription

import com.spotify.mobius.rx2.RxMobius
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.ObservableTransformer
import org.simple.clinic.drugs.PrescriptionRepository
import org.simple.clinic.patient.PatientRepository
import org.simple.clinic.signature.SignatureRepository
import org.simple.clinic.util.scheduler.SchedulersProvider

class TeleconsultSharePrescriptionEffectHandler @AssistedInject constructor(
    private val schedulersProvider: SchedulersProvider,
    private val patientRepository: PatientRepository,
    private val prescriptionRepository: PrescriptionRepository,
    private val signatureRepository: SignatureRepository,
    @Assisted private val uiActions: TeleconsultSharePrescriptionUiActions
) {

  @AssistedInject.Factory
  interface Factory {
    fun create(uiActions: TeleconsultSharePrescriptionUiActions): TeleconsultSharePrescriptionEffectHandler
  }

  fun build(): ObservableTransformer<TeleconsultSharePrescriptionEffect, TeleconsultSharePrescriptionEvent> {
    return RxMobius
        .subtypeEffectHandler<TeleconsultSharePrescriptionEffect, TeleconsultSharePrescriptionEvent>()
        .addTransformer(LoadPatientDetails::class.java, loadPatientDetails())
        .addTransformer(LoadPatientMedicines::class.java, loadPatientMedicines())
        .addTransformer(LoadSignature::class.java, loadSignature())
        .addConsumer(SetSignature::class.java, { uiActions.setSignatureBitmap(it.bitmap) }, schedulersProvider.ui())
        .build()
  }

  private fun loadSignature(): ObservableTransformer<LoadSignature, TeleconsultSharePrescriptionEvent> {
    return ObservableTransformer { effects ->
      effects
          .observeOn(schedulersProvider.io())
          .map { signatureRepository.getSignatureBitmap() }
          .map(::SignatureLoaded)
    }
  }

  private fun loadPatientMedicines(): ObservableTransformer<LoadPatientMedicines, TeleconsultSharePrescriptionEvent> {
    return ObservableTransformer { effects ->
      effects
          .observeOn(schedulersProvider.io())
          .map { prescriptionRepository.newestPrescriptionsForPatientImmediate(it.patientUuid) }
          .map(::PatientMedicinesLoaded)
    }
  }

  private fun loadPatientDetails(): ObservableTransformer<LoadPatientDetails, TeleconsultSharePrescriptionEvent> {
    return ObservableTransformer { effects ->
      effects
          .observeOn(schedulersProvider.io())
          .map { patientRepository.patientImmediate(it.patientUuid) }
          .map(::PatientDetailsLoaded)
    }
  }
}
