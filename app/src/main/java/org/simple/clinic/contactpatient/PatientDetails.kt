package org.simple.clinic.contactpatient

import org.simple.clinic.medicalhistory.Answer
import org.simple.clinic.patient.Gender
import java.time.Instant

data class PatientDetails(
    val name: String,
    val gender: Gender,
    val age: Int,
    val phoneNumber: String?,
    val patientAddress: String,
    val registeredFacility: String,
    val diagnosedWithDiabetes: Answer?,
    val diagnosedWithHypertension: Answer?,
    val lastVisited: Instant
)

enum class ResultLabel {
  RESULT_OF_CALL, RESULT
}
