package org.simple.clinic.drugs.search.sync

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface DrugSyncApi {

  @Headers(value = ["X-RESYNC-TOKEN: 1"])
  @GET("v4/medications/sync")
  fun pull(
      @Query("limit") recordsToPull: Int,
      @Query("process_token") lastPullToken: String? = null
  ): Call<DrugPullResponse>
}
