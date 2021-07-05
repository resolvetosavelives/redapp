package org.simple.clinic.patient.onlinelookup.api

import org.simple.clinic.medicalhistory.Answer
import org.simple.clinic.medicalhistory.MedicalHistory
import org.simple.clinic.overdue.Appointment
import org.simple.clinic.patient.Age
import org.simple.clinic.patient.CompleteMedicalRecord
import org.simple.clinic.patient.Patient
import org.simple.clinic.patient.PatientProfile
import org.simple.clinic.patient.SyncStatus
import org.simple.clinic.patient.onlinelookup.api.LookupPatientOnline.Result.Found
import org.simple.clinic.patient.onlinelookup.api.LookupPatientOnline.Result.NotFound
import org.simple.clinic.patient.onlinelookup.api.LookupPatientOnline.Result.OtherError
import org.simple.clinic.patient.sync.ForPatientSync
import org.simple.clinic.patient.sync.ForPatientSync.Type.RecordRetentionFallbackDuration
import org.simple.clinic.patient.sync.PatientSyncApi
import org.simple.clinic.platform.crash.CrashReporter
import org.simple.clinic.util.UtcClock
import retrofit2.Response
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

class LookupPatientOnline @Inject constructor(
    private val patientSyncApi: PatientSyncApi,
    private val clock: UtcClock,
    @ForPatientSync(RecordRetentionFallbackDuration) private val fallbackRecordRetentionDuration: Duration
) {

  fun lookupWithIdentifier(identifier: String): Result {
    return try {
      lookupPatientOnServer(identifier)
    } catch (e: Exception) {
      CrashReporter.report(e)
      OtherError
    }
  }

  private fun lookupPatientOnServer(identifier: String): Result {
    val response = patientSyncApi.lookup(PatientOnlineLookupRequest(identifier)).execute()

    return when (response.code()) {
      200 -> readSuccessResponse(response, identifier)
      404 -> NotFound(identifier)
      else -> OtherError
    }
  }

  private fun readSuccessResponse(
      response: Response<OnlineLookupResponsePayload>,
      identifier: String
  ): Result {
    val responseBody = response.body()!!
    val medicalRecords = responseBody.patients.map(::convertResponseToMedicalRecord)

    return if (medicalRecords.isNotEmpty()) Found(medicalRecords) else NotFound(identifier)
  }

  private fun convertResponseToMedicalRecord(
      response: CompleteMedicalRecordPayload
  ): CompleteMedicalRecord {
    val age = if (response.age != null) Age(response.age, response.ageUpdatedAt!!) else null
    val retainUntil = response.retention.computeRetainUntilTimestamp(
        instant = Instant.now(clock),
        fallbackRetentionDuration = fallbackRecordRetentionDuration
    )

    val phoneNumbers = response
        .phoneNumbers
        ?.map { payload ->
          payload.toDatabaseModel(response.id)
        } ?: emptyList()

    val businessIds = response
        .businessIds
        .map { payload ->
          payload.toDatabaseModel(response.id)
        }

    val patientProfile = PatientProfile(
        patient = Patient(
            uuid = response.id,
            fullName = response.fullName,
            gender = response.gender,
            dateOfBirth = response.dateOfBirth,
            age = age,
            createdAt = response.createdAt,
            updatedAt = response.updatedAt,
            deletedAt = response.deletedAt,
            addressUuid = response.address.uuid,
            status = response.status,
            recordedAt = response.recordedAt,
            syncStatus = SyncStatus.DONE,
            reminderConsent = response.reminderConsent,
            deletedReason = response.deletedReason,
            registeredFacilityId = response.registeredFacilityId,
            assignedFacilityId = response.assignedFacilityId,
            retainUntil = retainUntil
        ),
        address = response.address.toDatabaseModel(),
        phoneNumbers = phoneNumbers,
        businessIds = businessIds
    )

    val medicalHistory = if (response.medicalHistory != null) {
      MedicalHistory(
          uuid = response.medicalHistory.uuid,
          patientUuid = response.medicalHistory.patientUuid,
          diagnosedWithHypertension = response.medicalHistory.hasHypertension ?: Answer.Unanswered,
          isOnHypertensionTreatment = response.medicalHistory.isOnTreatmentForHypertension,
          hasHadHeartAttack = response.medicalHistory.hasHadHeartAttack,
          hasHadStroke = response.medicalHistory.hasHadStroke,
          hasHadKidneyDisease = response.medicalHistory.hasHadKidneyDisease,
          diagnosedWithDiabetes = response.medicalHistory.hasDiabetes,
          syncStatus = SyncStatus.DONE,
          createdAt = response.medicalHistory.createdAt,
          updatedAt = response.medicalHistory.updatedAt,
          deletedAt = response.medicalHistory.deletedAt
      )
    } else {
      null
    }

    val appointments = response.appointments.map { payload ->
      Appointment(
          uuid = payload.uuid,
          patientUuid = payload.patientUuid,
          facilityUuid = payload.facilityUuid,
          scheduledDate = payload.date,
          status = payload.status,
          cancelReason = payload.cancelReason,
          remindOn = payload.remindOn,
          agreedToVisit = payload.agreedToVisit,
          appointmentType = payload.appointmentType,
          syncStatus = SyncStatus.DONE,
          createdAt = payload.createdAt,
          updatedAt = payload.updatedAt,
          deletedAt = payload.deletedAt,
          creationFacilityUuid = payload.creationFacilityUuid)
    }

    val bloodPressures = response.bloodPressures.map { payload ->
      payload.toDatabaseModel(SyncStatus.DONE)
    }

    val bloodSugars = response.bloodSugars.map { payload ->
      payload.toDatabaseModel(SyncStatus.DONE)
    }

    val prescribedDrugs = response.prescribedDrugs.map { payload ->
      payload.toDatabaseModel(SyncStatus.DONE)
    }

    return CompleteMedicalRecord(
        patient = patientProfile,
        medicalHistory = medicalHistory,
        appointments = appointments,
        bloodSugars = bloodSugars,
        bloodPressures = bloodPressures,
        prescribedDrugs = prescribedDrugs
    )
  }

  sealed class Result {
    data class Found(val medicalRecords: List<CompleteMedicalRecord>) : Result()
    data class NotFound(val identifier: String) : Result()
    object OtherError : Result()
  }
}
