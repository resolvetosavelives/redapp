package org.simple.clinic.user

import android.content.SharedPreferences
import androidx.test.runner.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.simple.clinic.AppDatabase
import org.simple.clinic.TestClinicApp
import org.simple.clinic.TestData
import org.simple.clinic.util.Rules
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class UserDaoAndroidTest {

  @Inject
  lateinit var sharedPrefs: SharedPreferences

  @Inject
  lateinit var appDatabase: AppDatabase

  @Inject
  lateinit var testData: TestData

  @get:Rule
  val ruleChain: RuleChain = Rules.global()

  @Before
  fun setup() {
    TestClinicApp.appComponent().inject(this)
  }

  @After
  fun tearDown() {
    appDatabase.clearAllTables()
  }

  /**
   * This was added after we found that Room doesn't complain if incorrect values
   * are passed for @Insert's onConflict strategy and [User.RoomDao.createOrUpdate]
   * was ignoring updates.
   */
  @Test
  fun update_should_work_correctly() {
    val user = testData.loggedInUser(status = UserStatus.WaitingForApproval)
    val updatedUser = user.copy(status = UserStatus.ApprovedForSyncing)

    appDatabase.userDao().createOrUpdate(user)
    appDatabase.userDao().createOrUpdate(updatedUser)

    val updatedUserInDatabase = appDatabase.userDao().userImmediate()
    assertThat(updatedUserInDatabase).isEqualTo(updatedUser)
  }
}
