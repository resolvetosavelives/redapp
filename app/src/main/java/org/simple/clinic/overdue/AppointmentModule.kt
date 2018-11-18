package org.simple.clinic.overdue

import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import dagger.Module
import dagger.Provides
import io.reactivex.Single
import org.simple.clinic.AppDatabase
import org.simple.clinic.home.overdue.OverdueAppointment
import org.simple.clinic.util.InstantRxPreferencesConverter
import org.simple.clinic.util.None
import org.simple.clinic.util.Optional
import org.simple.clinic.util.OptionalRxPreferencesConverter
import org.threeten.bp.Instant
import retrofit2.Retrofit
import javax.inject.Named

@Module
class AppointmentModule {

  @Provides
  fun config(): Single<AppointmentConfig> {
    return Single.just(AppointmentConfig(highlightHighRiskPatients = false, v2ApiEnabled = false))
  }

  @Provides
  fun dao(appDatabase: AppDatabase): Appointment.RoomDao {
    return appDatabase.appointmentDao()
  }

  @Provides
  fun overdueAppointmentDao(appDatabase: AppDatabase): OverdueAppointment.RoomDao {
    return appDatabase.overdueAppointmentDao()
  }

  @Provides
  fun syncApiV1(retrofit: Retrofit): AppointmentSyncApiV1 {
    return retrofit.create(AppointmentSyncApiV1::class.java)
  }

  @Provides
  fun syncApiV2(retrofit: Retrofit): AppointmentSyncApiV2 {
    return retrofit.create(AppointmentSyncApiV2::class.java)
  }

  @Provides
  @Named("last_appointment_pull_timestamp")
  fun lastPullTimestamp(rxSharedPrefs: RxSharedPreferences): Preference<Optional<Instant>> {
    return rxSharedPrefs.getObject("last_appointment_pull_timestamp", None, OptionalRxPreferencesConverter(InstantRxPreferencesConverter()))
  }
}
