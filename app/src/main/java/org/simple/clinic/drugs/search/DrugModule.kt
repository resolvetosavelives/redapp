package org.simple.clinic.drugs.search

import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import dagger.Module
import dagger.Provides
import org.simple.clinic.AppDatabase
import org.simple.clinic.drugs.search.sync.DrugSyncApi
import org.simple.clinic.main.TypedPreference
import org.simple.clinic.main.TypedPreference.Type.LastDrugPullToken
import org.simple.clinic.util.preference.StringPreferenceConverter
import org.simple.clinic.util.preference.getOptional
import retrofit2.Retrofit
import java.util.Optional
import javax.inject.Named

@Module
object DrugModule {

  @Provides
  fun drugDao(database: AppDatabase): Drug.RoomDao {
    return database.drugDao()
  }

  @Provides
  fun drugSyncApi(@Named("for_country") retrofit: Retrofit): DrugSyncApi {
    return retrofit.create(DrugSyncApi::class.java)
  }

  @Provides
  @TypedPreference(LastDrugPullToken)
  fun lastPullToken(rxSharedPreferences: RxSharedPreferences): Preference<Optional<String>> {
    return rxSharedPreferences.getOptional("last_drug_pull_timestamp", StringPreferenceConverter())
  }
}
