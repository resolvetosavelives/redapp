package org.simple.clinic.drugs.search.sync

import com.f2prateek.rx.preferences2.Preference
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.simple.clinic.AppDatabase
import org.simple.clinic.TestClinicApp
import org.simple.clinic.drugs.search.DrugRepository
import org.simple.clinic.main.TypedPreference
import org.simple.clinic.main.TypedPreference.Type.LastDrugPullToken
import org.simple.clinic.rules.ServerAuthenticationRule
import org.simple.clinic.sync.SyncConfig
import org.simple.clinic.sync.SyncCoordinator
import org.simple.clinic.sync.SyncGroup
import org.simple.clinic.sync.SyncInterval
import org.simple.clinic.util.Rules
import java.util.Optional
import javax.inject.Inject

class DrugSyncIntegrationTest {
  @Inject
  lateinit var appDatabase: AppDatabase

  @Inject
  lateinit var repository: DrugRepository

  @Inject
  @TypedPreference(LastDrugPullToken)
  lateinit var lastPullToken: Preference<Optional<String>>

  @Inject
  lateinit var syncApi: DrugSyncApi

  @get:Rule
  val ruleChain: RuleChain = Rules
      .global()
      .around(ServerAuthenticationRule())

  private lateinit var sync: DrugSync

  // Currently the medications API returns all the drugs and doesn't have
  // support for batch sizes. So setting a small batch size causes the API
  // call to loop.
  private val batchSize = 1000
  private val config = SyncConfig(
      syncInterval = SyncInterval.FREQUENT,
      batchSize = batchSize,
      syncGroup = SyncGroup.FREQUENT
  )

  @Before
  fun setUp() {
    TestClinicApp.appComponent().inject(this)

    resetLocalData()

    sync = DrugSync(
        syncCoordinator = SyncCoordinator(),
        repository = repository,
        api = syncApi,
        lastPullToken = lastPullToken,
        config = config
    )
  }

  @After
  fun tearDown() {
    resetLocalData()
  }

  private fun resetLocalData() {
    appDatabase.clearAllTables()
    lastPullToken.delete()
  }

  @Test
  fun syncing_records_should_work_as_expected() {
    // when
    assertThat(repository.recordCount().blockingFirst()).isEqualTo(0)
    sync.pull()

    // then
    val pulledRecords = repository.drugs()

    assertThat(pulledRecords).isNotEmpty()
  }
}
