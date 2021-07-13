package org.simple.clinic.drugs.search

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.simple.clinic.AppDatabase
import org.simple.clinic.TestClinicApp
import org.simple.clinic.TestData
import java.util.UUID
import javax.inject.Inject

class DrugRepositoryAndroidTest {

  @Inject
  lateinit var database: AppDatabase

  @Inject
  lateinit var drugRepository: DrugRepository

  @Before
  fun setUp() {
    TestClinicApp.appComponent().inject(this)
  }

  @After
  fun tearDown() {
    database.clearAllTables()
  }

  @Test
  fun saving_drugs_should_work_correctly() {
    // given
    val drugs = listOf(
        TestData.drug(
            id = UUID.fromString("4e64d256-ea7e-4b90-8177-1eb1485630c3"),
            name = "Amlodipine",
            dosage = "10 mg"
        ),
        TestData.drug(
            id = UUID.fromString("0864635d-fd63-4005-9cea-ba843ea804e6"),
            name = "Amlodipine",
            dosage = "20 mg"
        )
    )

    // when
    drugRepository.save(drugs).blockingAwait()

    // then
    val savedDrugs = drugRepository.drugs()

    assertThat(savedDrugs).isEqualTo(drugs)
  }
}
