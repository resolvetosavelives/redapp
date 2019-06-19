package org.simple.clinic.allpatientsinfacility

import org.simple.clinic.facility.Facility
import org.simple.clinic.patient.PatientSearchResult

interface AllPatientsInFacilityView {
  fun showNoPatientsFound(facilityName: String)
  fun showPatients(facility: Facility, patientSearchResults: List<PatientSearchResult>)
}
