package org.simple.clinic.summary

import org.simple.clinic.mobius.ViewRenderer
import org.simple.clinic.summary.teleconsultation.api.TeleconsultInfo

class PatientSummaryViewRenderer(
    private val ui: PatientSummaryScreenUi
) : ViewRenderer<PatientSummaryModel> {

  override fun render(model: PatientSummaryModel) {
    with(ui) {
      if (model.hasLoadedPatientSummaryProfile) {
        populatePatientProfile(model.patientSummaryProfile!!)
        showEditButton()
        setupUiForAssignedFacility(model)
      }

      if (model.hasLoadedCurrentFacility) {
        setupUiForDiabetesManagement(model.isDiabetesManagementEnabled)
        setupUiForTeleconsult(model)
      }
    }
  }

  private fun setupUiForAssignedFacility(model: PatientSummaryModel) {
    if (model.hasAssignedFacility) {
      ui.showAssignedFacilityView()
    } else {
      ui.hideAssignedFacilityView()
    }
  }

  private fun setupUiForTeleconsult(model: PatientSummaryModel) {
    if (model.openIntention is OpenIntention.ViewExistingPatientWithTeleconsultLog) {
      renderMedicalOfficerView()
    } else {
      renderUserView(model)
    }
  }

  private fun renderMedicalOfficerView() {
    ui.hideContactDoctorButton()
    ui.hideDoneButton()
    ui.showTeleconsultLogButton()
  }

  private fun renderUserView(model: PatientSummaryModel) {
    if (model.isTeleconsultationEnabled && model.isUserLoggedIn) {
      renderContactDoctorButton(model)
    } else {
      ui.hideContactDoctorButton()
    }
  }

  private fun renderContactDoctorButton(model: PatientSummaryModel) {
    ui.showContactDoctorButton()
    when (model.teleconsultInfo) {
      is TeleconsultInfo.Fetched -> ui.enableContactDoctorButton()
      is TeleconsultInfo.MissingPhoneNumber, is TeleconsultInfo.NetworkError -> ui.disableContactDoctorButton()
      is TeleconsultInfo.Fetching -> ui.fetchingTeleconsultInfo()
    }
  }

  private fun setupUiForDiabetesManagement(isDiabetesManagementEnabled: Boolean) {
    if (isDiabetesManagementEnabled) {
      ui.showDiabetesView()
    } else {
      ui.hideDiabetesView()
    }
  }
}
