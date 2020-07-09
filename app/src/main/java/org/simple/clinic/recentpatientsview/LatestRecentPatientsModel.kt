package org.simple.clinic.recentpatientsview

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.simple.clinic.patient.RecentPatient

@Parcelize
data class LatestRecentPatientsModel(
    val recentPatients: List<RecentPatient>?
) : Parcelable {

  companion object {
    fun create(): LatestRecentPatientsModel = LatestRecentPatientsModel(recentPatients = null)
  }

  val hasLoadedRecentPatients: Boolean
    get() = recentPatients != null

  fun recentPatientsLoaded(recentPatients: List<RecentPatient>): LatestRecentPatientsModel {
    return copy(recentPatients = recentPatients)
  }
}
