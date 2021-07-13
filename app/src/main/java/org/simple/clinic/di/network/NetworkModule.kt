package org.simple.clinic.di.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import dagger.Module
import dagger.Provides
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.simple.clinic.bloodsugar.BloodSugarMeasurementType
import org.simple.clinic.di.AppScope
import org.simple.clinic.drugs.search.DrugCategory
import org.simple.clinic.medicalhistory.Answer
import org.simple.clinic.overdue.Appointment
import org.simple.clinic.overdue.AppointmentCancelReason
import org.simple.clinic.patient.DeletedReason
import org.simple.clinic.patient.Gender
import org.simple.clinic.patient.PatientPhoneNumberType
import org.simple.clinic.patient.PatientStatus
import org.simple.clinic.patient.ReminderConsent
import org.simple.clinic.patient.businessid.BusinessId
import org.simple.clinic.patient.businessid.Identifier
import org.simple.clinic.patient.onlinelookup.api.DurationFromSecondsMoshiAdapter
import org.simple.clinic.patient.onlinelookup.api.RetentionType
import org.simple.clinic.patient.sync.PatientPayload
import org.simple.clinic.remoteconfig.ConfigReader
import org.simple.clinic.scanid.IndiaNHIDDateOfBirthMoshiAdapter
import org.simple.clinic.scanid.IndiaNHIDGender
import org.simple.clinic.teleconsultlog.medicinefrequency.MedicineFrequency
import org.simple.clinic.teleconsultlog.teleconsultrecord.TeleconsultStatus
import org.simple.clinic.teleconsultlog.teleconsultrecord.TeleconsultationType
import org.simple.clinic.user.User
import org.simple.clinic.user.UserStatus
import org.simple.clinic.util.moshi.InstantMoshiAdapter
import org.simple.clinic.util.moshi.LocalDateMoshiAdapter
import org.simple.clinic.util.moshi.MoshiOptionalAdapterFactory
import org.simple.clinic.util.moshi.URIMoshiAdapter
import org.simple.clinic.util.moshi.UuidMoshiAdapter
import java.util.concurrent.TimeUnit
import org.simple.clinic.drugs.search.Answer as DrugAnswer
import org.simple.clinic.teleconsultlog.teleconsultrecord.Answer as TeleconsultAnswer

@Module
class NetworkModule {

  @Provides
  @AppScope
  fun moshi(): Moshi {
    val moshi = Moshi.Builder()
        .add(InstantMoshiAdapter())
        .add(LocalDateMoshiAdapter())
        .add(UuidMoshiAdapter())
        .add(MoshiOptionalAdapterFactory())
        .add(AppointmentCancelReason.MoshiTypeConverter())
        .add(Identifier.IdentifierType.MoshiTypeAdapter())
        .add(BusinessId.MetaDataVersion.MoshiTypeAdapter())
        .add(Appointment.AppointmentType.MoshiTypeAdapter())
        .add(UserStatus.MoshiTypeConverter())
        .add(Appointment.Status.MoshiTypeConverter())
        .add(PatientStatus.MoshiTypeAdapter())
        .add(ReminderConsent.MoshiTypeAdapter())
        .add(Answer.MoshiTypeAdapter())
        .add(Gender.MoshiTypeAdapter())
        .add(PatientPhoneNumberType.MoshiTypeAdapter())
        .add(URIMoshiAdapter())
        .add(BloodSugarMeasurementType.MoshiTypeAdapter())
        .add(DeletedReason.MoshiTypeConverter())
        .add(User.CapabilityStatus.MoshiTypeAdapter())
        .add(MedicineFrequency.MoshiTypeConverter())
        .add(TeleconsultAnswer.MoshiTypeAdapter())
        .add(TeleconsultationType.MoshiTypeAdapter())
        .add(TeleconsultStatus.MoshiTypeAdapter())
        .add(IndiaNHIDDateOfBirthMoshiAdapter())
        .add(IndiaNHIDGender.MoshiTypeAdapter())
        .add(DurationFromSecondsMoshiAdapter())
        .add(RetentionType::class.java, EnumJsonAdapter.create(RetentionType::class.java).withUnknownFallback(RetentionType.Unknown))
        .add(DrugCategory.MoshiTypeConverter())
        .add(DrugAnswer.MoshiTypeAdapter())
        .build()

    val patientPayloadNullSerializingAdapter = moshi.adapter(PatientPayload::class.java).serializeNulls()

    return moshi
        .newBuilder()
        .add(PatientPayload::class.java, patientPayloadNullSerializingAdapter)
        .build()
  }

  @Provides
  @AppScope
  fun okHttpClient(
      interceptors: List<@JvmSuppressWildcards Interceptor>,
      configReader: ConfigReader
  ): OkHttpClient {
    return OkHttpClient.Builder()
        .apply {
          interceptors.forEach { addInterceptor(it) }

          // When syncing large amounts of data, the default read timeout(10s) has been seen to
          // timeout frequently for larger models. Through trial and error, 15s was found to be a
          // good number for syncing large batch sizes.
          readTimeout(configReader.long("networkmodule_read_timeout", default = 30L), TimeUnit.SECONDS)
        }
        .build()
  }
}
