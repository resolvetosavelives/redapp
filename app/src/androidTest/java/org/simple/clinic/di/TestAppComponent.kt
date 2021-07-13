package org.simple.clinic.di

import dagger.Component
import org.simple.clinic.DateOfBirthHintUnfocusedAndroidTest
import org.simple.clinic.FakerModule
import org.simple.clinic.TestClinicApp
import org.simple.clinic.TestDataModule
import org.simple.clinic.appconfig.SelectedCountryPersistenceAndroidTest
import org.simple.clinic.bloodsugar.BloodSugarHistoryListItemDataSourceTest
import org.simple.clinic.bloodsugar.BloodSugarRepositoryAndroidTest
import org.simple.clinic.bp.BloodPressureHistoryListItemDataSourceTest
import org.simple.clinic.bp.BloodPressureRepositoryAndroidTest
import org.simple.clinic.drugs.PrescriptionRepositoryAndroidTest
import org.simple.clinic.drugs.search.DrugRepositoryAndroidTest
import org.simple.clinic.drugs.search.sync.DrugSyncIntegrationTest
import org.simple.clinic.facility.FacilityRepositoryAndroidTest
import org.simple.clinic.login.LoginUserWithOtpServerIntegrationTest
import org.simple.clinic.medicalhistory.MedicalHistoryRepositoryAndroidTest
import org.simple.clinic.overdue.AppointmentRepositoryAndroidTest
import org.simple.clinic.patient.PatientRepositoryAndroidTest
import org.simple.clinic.patient.onlinelookup.api.LookupPatientOnlineApiIntegrationTest
import org.simple.clinic.protocolv2.ProtocolRepositoryAndroidTest
import org.simple.clinic.protocolv2.sync.ProtocolSyncAndroidTest
import org.simple.clinic.rules.LocalAuthenticationRule
import org.simple.clinic.rules.RegisterPatientRule
import org.simple.clinic.rules.ServerAuthenticationRule
import org.simple.clinic.rules.ServerRegistrationAtFacilityRule
import org.simple.clinic.security.pin.BruteForceProtectionAndroidTest
import org.simple.clinic.signature.SignatureRepositoryAndroidTest
import org.simple.clinic.storage.DaoWithUpsertAndroidTest
import org.simple.clinic.storage.DeleteSyncGroupDatabaseAndroidTest
import org.simple.clinic.storage.PurgeDatabaseAndroidTest
import org.simple.clinic.storage.migrations.BaseDatabaseMigrationTest
import org.simple.clinic.storage.migrations.DatabaseMigrationAndroidTest
import org.simple.clinic.storage.migrations.Migration57AndroidTest
import org.simple.clinic.storage.migrations.Migration58AndroidTest
import org.simple.clinic.storage.migrations.Migration59AndroidTest
import org.simple.clinic.summary.addphone.MissingPhoneReminderRepositoryAndroidTest
import org.simple.clinic.summary.teleconsultation.sync.TeleconsultationFacilityRepositoryAndroidTest
import org.simple.clinic.summary.teleconsultation.sync.TeleconsultationFacilityWithMedicalOfficersTest
import org.simple.clinic.sync.AppointmentSyncIntegrationTest
import org.simple.clinic.sync.BloodPressureSyncIntegrationTest
import org.simple.clinic.sync.BloodSugarSyncIntegrationTest
import org.simple.clinic.sync.HelpSyncIntegrationTest
import org.simple.clinic.sync.MedicalHistorySyncIntegrationTest
import org.simple.clinic.sync.PatientSyncIntegrationTest
import org.simple.clinic.sync.PrescriptionSyncIntegrationTest
import org.simple.clinic.sync.ProtocolSyncIntegrationTest
import org.simple.clinic.sync.ReportsSyncIntegrationTest
import org.simple.clinic.sync.TeleconsultationSyncIntegrationTest
import org.simple.clinic.teleconsultlog.teleconsultrecord.TeleconsultRecordRepositoryAndroidTest
import org.simple.clinic.teleconsultlog.teleconsultrecord.TeleconsultRecordSyncIntegrationTest
import org.simple.clinic.teleconsultlog.teleconsultrecord.TeleconsultRecordTest
import org.simple.clinic.user.OngoingLoginEntryRepositoryTest
import org.simple.clinic.user.RegisterUserServerIntegrationTest
import org.simple.clinic.user.UserDaoAndroidTest
import org.simple.clinic.user.UserSessionAndroidTest

@AppScope
@Component(
    modules = [
      TestAppModule::class,
      FakerModule::class,
      TestDataModule::class
    ]
)
interface TestAppComponent {

  fun inject(target: TestClinicApp)
  fun inject(target: UserSessionAndroidTest)
  fun inject(target: ProtocolSyncAndroidTest)
  fun inject(target: PatientRepositoryAndroidTest)
  fun inject(target: PrescriptionRepositoryAndroidTest)
  fun inject(target: FacilityRepositoryAndroidTest)
  fun inject(target: UserDaoAndroidTest)
  fun inject(target: AppointmentRepositoryAndroidTest)
  fun inject(target: LocalAuthenticationRule)
  fun inject(target: MedicalHistoryRepositoryAndroidTest)
  fun inject(target: BloodPressureRepositoryAndroidTest)
  fun inject(target: OngoingLoginEntryRepositoryTest)
  fun inject(target: BruteForceProtectionAndroidTest)
  fun inject(target: DaoWithUpsertAndroidTest)
  fun inject(target: ProtocolRepositoryAndroidTest)
  fun inject(target: RegisterPatientRule)
  fun inject(target: DatabaseMigrationAndroidTest)
  fun inject(target: ReportsSyncIntegrationTest)
  fun inject(target: MissingPhoneReminderRepositoryAndroidTest)
  fun inject(target: HelpSyncIntegrationTest)
  fun inject(target: ServerAuthenticationRule)
  fun inject(target: RegisterUserServerIntegrationTest)
  fun inject(target: SelectedCountryPersistenceAndroidTest)
  fun inject(target: LoginUserWithOtpServerIntegrationTest)
  fun inject(target: DateOfBirthHintUnfocusedAndroidTest)
  fun inject(target: BaseDatabaseMigrationTest)
  fun inject(target: BloodSugarRepositoryAndroidTest)
  fun inject(target: Migration57AndroidTest)
  fun inject(target: Migration58AndroidTest)
  fun inject(target: Migration59AndroidTest)
  fun inject(target: BloodPressureHistoryListItemDataSourceTest)
  fun inject(target: BloodSugarHistoryListItemDataSourceTest)
  fun inject(target: TeleconsultationFacilityWithMedicalOfficersTest)
  fun inject(target: TeleconsultationFacilityRepositoryAndroidTest)
  fun inject(target: PatientSyncIntegrationTest)
  fun inject(target: BloodPressureSyncIntegrationTest)
  fun inject(target: MedicalHistorySyncIntegrationTest)
  fun inject(target: AppointmentSyncIntegrationTest)
  fun inject(target: PrescriptionSyncIntegrationTest)
  fun inject(target: BloodSugarSyncIntegrationTest)
  fun inject(target: TeleconsultationSyncIntegrationTest)
  fun inject(target: ProtocolSyncIntegrationTest)
  fun inject(target: TeleconsultRecordTest)
  fun inject(target: PurgeDatabaseAndroidTest)
  fun inject(target: SignatureRepositoryAndroidTest)
  fun inject(target: TeleconsultRecordRepositoryAndroidTest)
  fun inject(target: TeleconsultRecordSyncIntegrationTest)
  fun inject(target: DeleteSyncGroupDatabaseAndroidTest)
  fun inject(target: ServerRegistrationAtFacilityRule)
  fun inject(target: LookupPatientOnlineApiIntegrationTest)
  fun inject(target: DrugRepositoryAndroidTest)
  fun inject(target: DrugSyncIntegrationTest)
}
