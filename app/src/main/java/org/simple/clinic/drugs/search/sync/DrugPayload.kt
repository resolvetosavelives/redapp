package org.simple.clinic.drugs.search.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.simple.clinic.drugs.search.Answer
import org.simple.clinic.drugs.search.Drug
import org.simple.clinic.drugs.search.DrugCategory
import org.simple.clinic.storage.Timestamps
import org.simple.clinic.teleconsultlog.medicinefrequency.MedicineFrequency
import java.time.Instant
import java.util.UUID

@JsonClass(generateAdapter = true)
data class DrugPayload(

    @Json(name = "id")
    val uuid: UUID,

    @Json(name = "name")
    val name: String,

    @Json(name = "category")
    val category: DrugCategory?,

    @Json(name = "frequency")
    val frequency: MedicineFrequency?,

    @Json(name = "composition")
    val composition: String?,

    @Json(name = "dosage")
    val dosage: String?,

    @Json(name = "rxnorm_code")
    val rxNormCode: String?,

    @Json(name = "protocol")
    val protocol: Answer,

    @Json(name = "common")
    val common: Answer,

    @Json(name = "created_at")
    val createdAt: Instant,

    @Json(name = "updated_at")
    val updatedAt: Instant,

    @Json(name = "deleted_at")
    val deletedAt: Instant?
) {

  fun toDatabaseModel(): Drug {
    return Drug(
        id = uuid,
        name = name,
        category = category,
        frequency = frequency,
        composition = composition,
        dosage = dosage,
        rxNormCode = rxNormCode,
        protocol = protocol,
        common = common,
        timestamps = Timestamps(
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt
        )
    )
  }
}
