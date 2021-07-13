package org.simple.clinic.drugs.search

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import io.reactivex.Observable
import org.simple.clinic.storage.Timestamps
import org.simple.clinic.teleconsultlog.medicinefrequency.MedicineFrequency
import java.util.UUID

@Entity
data class Drug(
    @PrimaryKey
    val id: UUID,

    val name: String,

    val category: DrugCategory?,

    val frequency: MedicineFrequency?,

    val composition: String?,

    val dosage: String?,

    val rxNormCode: String?,

    val protocol: Answer,

    val common: Answer,

    @Embedded
    val timestamps: Timestamps
) {

  @Dao
  interface RoomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(drugs: List<Drug>)

    @Query("SELECT * FROM Drug")
    fun getAll(): List<Drug>

    @Query("SELECT COUNT(uuid) FROM Protocol")
    fun count(): Observable<Int>
  }
}
