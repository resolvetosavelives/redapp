package org.simple.clinic.drugs.search

import io.reactivex.Completable
import io.reactivex.Observable
import org.simple.clinic.drugs.search.sync.DrugPayload
import org.simple.clinic.patient.SyncStatus
import org.simple.clinic.sync.SynceableRepository
import java.util.UUID
import javax.inject.Inject

class DrugRepository @Inject constructor(
    private val drugDao: Drug.RoomDao
) : SynceableRepository<Drug, DrugPayload> {

  override fun save(records: List<Drug>): Completable {
    return Completable.fromAction { saveRecords(records) }
  }

  override fun setSyncStatus(from: SyncStatus, to: SyncStatus) {
    // no-op
  }

  override fun setSyncStatus(ids: List<UUID>, to: SyncStatus) {
    // no-op
  }

  override fun mergeWithLocalData(payloads: List<DrugPayload>) {
    val records = payloads
        .map { it.toDatabaseModel() }

    saveRecords(records)
  }

  override fun recordCount(): Observable<Int> {
    return drugDao.count()
  }

  override fun pendingSyncRecordCount(): Observable<Int> {
    return Observable.just(0)
  }

  fun drugs(): List<Drug> {
    return drugDao.getAll()
  }

  private fun saveRecords(records: List<Drug>) {
    drugDao.save(records)
  }

  override fun pendingSyncRecords(limit: Int, offset: Int): List<Drug> {
    return emptyList()
  }
}
