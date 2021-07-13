package org.simple.clinic.drugs.search.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.simple.clinic.sync.DataPullResponse

@JsonClass(generateAdapter = true)
data class DrugPullResponse(

    @Json(name = "medications")
    override val payloads: List<DrugPayload>,

    @Json(name = "process_token")
    override val processToken: String

) : DataPullResponse<DrugPayload>
