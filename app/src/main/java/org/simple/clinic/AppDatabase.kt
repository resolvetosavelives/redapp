package org.simple.clinic

import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.simple.clinic.bloodsugar.BloodSugarMeasurement
import org.simple.clinic.bloodsugar.BloodSugarMeasurementType
import org.simple.clinic.bp.BloodPressureMeasurement
import org.simple.clinic.drugs.PrescribedDrug
import org.simple.clinic.drugs.search.Drug
import org.simple.clinic.drugs.search.DrugCategory
import org.simple.clinic.facility.Facility
import org.simple.clinic.home.overdue.OverdueAppointment
import org.simple.clinic.medicalhistory.Answer
import org.simple.clinic.medicalhistory.MedicalHistory
import org.simple.clinic.overdue.Appointment
import org.simple.clinic.overdue.AppointmentCancelReason
import org.simple.clinic.patient.DeletedReason
import org.simple.clinic.patient.Gender
import org.simple.clinic.patient.Patient
import org.simple.clinic.patient.PatientAddress
import org.simple.clinic.patient.PatientPhoneNumber
import org.simple.clinic.patient.PatientPhoneNumberType
import org.simple.clinic.patient.PatientSearchResult
import org.simple.clinic.patient.PatientStatus
import org.simple.clinic.patient.RecentPatient
import org.simple.clinic.patient.ReminderConsent
import org.simple.clinic.patient.SyncStatus
import org.simple.clinic.patient.businessid.BusinessId
import org.simple.clinic.patient.businessid.Identifier
import org.simple.clinic.platform.analytics.Analytics
import org.simple.clinic.platform.analytics.DatabaseOptimizationEvent
import org.simple.clinic.platform.analytics.DatabaseOptimizationEvent.OptimizationType.PurgeDeleted
import org.simple.clinic.platform.analytics.DatabaseOptimizationEvent.OptimizationType.PurgeFromOtherSyncGroup
import org.simple.clinic.platform.crash.CrashReporter
import org.simple.clinic.protocol.Protocol
import org.simple.clinic.protocol.ProtocolDrug
import org.simple.clinic.storage.text.TextRecord
import org.simple.clinic.summary.addphone.MissingPhoneReminder
import org.simple.clinic.summary.teleconsultation.sync.MedicalOfficer
import org.simple.clinic.summary.teleconsultation.sync.TeleconsultationFacilityInfo
import org.simple.clinic.summary.teleconsultation.sync.TeleconsultationFacilityMedicalOfficersCrossRef
import org.simple.clinic.summary.teleconsultation.sync.TeleconsultationFacilityWithMedicalOfficers
import org.simple.clinic.teleconsultlog.medicinefrequency.MedicineFrequency
import org.simple.clinic.teleconsultlog.teleconsultrecord.Answer.RoomTypeConverter
import org.simple.clinic.teleconsultlog.teleconsultrecord.TeleconsultRecord
import org.simple.clinic.teleconsultlog.teleconsultrecord.TeleconsultStatus
import org.simple.clinic.teleconsultlog.teleconsultrecord.TeleconsultationType
import org.simple.clinic.user.OngoingLoginEntry
import org.simple.clinic.user.User
import org.simple.clinic.user.UserStatus
import org.simple.clinic.util.room.InstantRoomTypeConverter
import org.simple.clinic.util.room.LocalDateRoomTypeConverter
import org.simple.clinic.util.room.UuidRoomTypeConverter
import java.time.Instant
import org.simple.clinic.drugs.search.Answer as DrugAnswer

@Database(
    entities = [
      Patient::class,
      PatientAddress::class,
      PatientPhoneNumber::class,
      BloodPressureMeasurement::class,
      PrescribedDrug::class,
      Facility::class,
      User::class,
      Appointment::class,
      MedicalHistory::class,
      OngoingLoginEntry::class,
      Protocol::class,
      ProtocolDrug::class,
      BusinessId::class,
      MissingPhoneReminder::class,
      BloodSugarMeasurement::class,
      TextRecord::class,
      TeleconsultationFacilityInfo::class,
      MedicalOfficer::class,
      TeleconsultationFacilityMedicalOfficersCrossRef::class,
      TeleconsultRecord::class,
      Drug::class
    ],
    views = [
      OverdueAppointment::class,
      PatientSearchResult::class
    ],
    version = 92,
    exportSchema = true
)
@TypeConverters(
    Gender.RoomTypeConverter::class,
    PatientPhoneNumberType.RoomTypeConverter::class,
    PatientStatus.RoomTypeConverter::class,
    SyncStatus.RoomTypeConverter::class,
    UserStatus.RoomTypeConverter::class,
    User.LoggedInStatus.RoomTypeConverter::class,
    Appointment.Status.RoomTypeConverter::class,
    AppointmentCancelReason.RoomTypeConverter::class,
    Answer.RoomTypeConverter::class,
    InstantRoomTypeConverter::class,
    LocalDateRoomTypeConverter::class,
    UuidRoomTypeConverter::class,
    Identifier.IdentifierType.RoomTypeConverter::class,
    BusinessId.MetaDataVersion.RoomTypeConverter::class,
    Appointment.AppointmentType.RoomTypeConverter::class,
    PatientStatus.RoomTypeConverter::class,
    ReminderConsent.RoomTypeConverter::class,
    BloodSugarMeasurementType.RoomTypeConverter::class,
    DeletedReason.RoomTypeConverter::class,
    MedicineFrequency.RoomTypeConverter::class,
    TeleconsultationType.RoomTypeConverter::class,
    RoomTypeConverter::class,
    User.CapabilityStatus.RoomTypeConverter::class,
    TeleconsultStatus.RoomTypeConverter::class,
    DrugCategory.RoomTypeConverter::class,
    DrugAnswer.RoomTypeConverter::class
)
abstract class AppDatabase : RoomDatabase() {

  abstract fun patientDao(): Patient.RoomDao

  abstract fun addressDao(): PatientAddress.RoomDao

  abstract fun phoneNumberDao(): PatientPhoneNumber.RoomDao

  abstract fun patientSearchDao(): PatientSearchResult.RoomDao

  abstract fun bloodPressureDao(): BloodPressureMeasurement.RoomDao

  abstract fun prescriptionDao(): PrescribedDrug.RoomDao

  abstract fun facilityDao(): Facility.RoomDao

  abstract fun userDao(): User.RoomDao

  abstract fun appointmentDao(): Appointment.RoomDao

  abstract fun overdueAppointmentDao(): OverdueAppointment.RoomDao

  abstract fun medicalHistoryDao(): MedicalHistory.RoomDao

  abstract fun ongoingLoginEntryDao(): OngoingLoginEntry.RoomDao

  abstract fun protocolDao(): Protocol.RoomDao

  abstract fun protocolDrugDao(): ProtocolDrug.RoomDao

  abstract fun recentPatientDao(): RecentPatient.RoomDao

  abstract fun businessIdDao(): BusinessId.RoomDao

  abstract fun missingPhoneReminderDao(): MissingPhoneReminder.RoomDao

  abstract fun bloodSugarDao(): BloodSugarMeasurement.RoomDao

  abstract fun textRecordDao(): TextRecord.RoomDao

  abstract fun teleconsultFacilityInfoDao(): TeleconsultationFacilityInfo.RoomDao

  abstract fun teleconsultMedicalOfficersDao(): MedicalOfficer.RoomDao

  abstract fun teleconsultFacilityWithMedicalOfficersDao(): TeleconsultationFacilityWithMedicalOfficers.RoomDao

  abstract fun teleconsultRecordDao(): TeleconsultRecord.RoomDao

  abstract fun drugDao(): Drug.RoomDao

  fun clearAppData() {
    runInTransaction {
      patientDao().clear()
      phoneNumberDao().clear()
      addressDao().clear()
      bloodPressureDao().clearData()
      prescriptionDao().clearData()
      appointmentDao().clear()
      medicalHistoryDao().clear()
      bloodSugarDao().clear()
      teleconsultFacilityInfoDao().clear()
      teleconsultMedicalOfficersDao().clear()
      teleconsultFacilityWithMedicalOfficersDao().clear()
      teleconsultRecordDao().clear()
    }
  }

  fun prune(now: Instant) {
    optimizeWithAnalytics(PurgeDeleted) {
      purge(now)
      try {
        vacuumDatabase()
      } catch (e: Exception) {
        // Vacuuming is an optimization that's unlikely to fail. But if it
        // does, we can ignore it and just report the exception and let
        // the original sqlite file continue to be used.
        CrashReporter.report(e)
      }
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  fun purge(now: Instant) {
    runInTransaction {
      withPatientDao(now)
      withBloodPressureDao()
      withBloodSugarDao()
      withAppointmentDao()
      withMedicalHistoryDao()
      withPrescriptionDao()
    }
  }

  private fun withPrescriptionDao() {
    with(prescriptionDao()) {
      purgeDeleted()
      purgePrescribedDrugWhenPatientIsNull()
    }
  }

  private fun withMedicalHistoryDao() {
    with(medicalHistoryDao()) {
      purgeDeleted()
      purgeMedicalHistoryWhenPatientIsNull()
    }
  }

  private fun withAppointmentDao() {
    with(appointmentDao()) {
      purgeDeleted()
      purgeUnusedAppointments()
      purgeAppointmentsWhenPatientIsNull()
    }
  }

  private fun withBloodSugarDao() {
    with(bloodSugarDao()) {
      purgeDeleted()
      purgeBloodSugarMeasurementWhenPatientIsNull()
    }
  }

  private fun withBloodPressureDao() {
    with(bloodPressureDao()) {
      purgeDeleted()
      purgeBloodPressureMeasurementWhenPatientIsNull()
    }
  }

  private fun withPatientDao(now: Instant) {
    with(patientDao()) {
      purgeDeleted()
      purgeDeletedPhoneNumbers()
      purgeDeletedBusinessIds()
      purgePatientAfterRetentionTime(now)
    }
  }

  private fun vacuumDatabase() {
    val db = openHelper.writableDatabase

    // Transfer everything from the write ahead log to the main database
    // file before running a vacuum.
    db.query("PRAGMA wal_checkpoint(FULL)").close()
    if (!db.inTransaction()) {
      db.execSQL("VACUUM")
    }
  }

  fun deletePatientsNotInFacilitySyncGroup(currentFacility: Facility) {
    optimizeWithAnalytics(PurgeFromOtherSyncGroup) {
      runInTransaction {
        val facilityIdsInCurrentSyncGroup = facilityDao().facilityIdsInSyncGroup(currentFacility.syncGroup)

        patientDao().deletePatientsNotInFacilities(facilityIdsInCurrentSyncGroup)
        bloodPressureDao().deleteWithoutLinkedPatient()
        bloodSugarDao().deleteWithoutLinkedPatient()
        appointmentDao().deleteWithoutLinkedPatient()
        prescriptionDao().deleteWithoutLinkedPatient()
        medicalHistoryDao().deleteWithoutLinkedPatient()
      }
    }
  }

  @WorkerThread
  fun sizeInBytes(): Long {
    return openHelper
        .readableDatabase
        .query(""" SELECT page_count * page_size as size FROM pragma_page_count(), pragma_page_size() """)
        .use { cursor ->
          cursor.moveToFirst()

          cursor.getLong(0)
        }
  }

  private inline fun optimizeWithAnalytics(
      type: DatabaseOptimizationEvent.OptimizationType,
      block: () -> Unit
  ) {
    val sizeBeforeOptimization = sizeInBytes()

    block.invoke()

    val sizeAfterOptimization = sizeInBytes()

    Analytics.reportDatabaseOptimizationEvent(DatabaseOptimizationEvent(
        sizeBeforeOptimizationBytes = sizeBeforeOptimization,
        sizeAfterOptimizationBytes = sizeAfterOptimization,
        type = type
    ))
  }
}
