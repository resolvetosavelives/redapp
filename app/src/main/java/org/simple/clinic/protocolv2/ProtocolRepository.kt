package org.simple.clinic.protocolv2

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.simple.clinic.AppDatabase
import org.simple.clinic.patient.SyncStatus
import org.simple.clinic.patient.canBeOverriddenByServerCopy
import org.simple.clinic.protocolv2.sync.ProtocolPayload
import org.simple.clinic.storage.inTransaction
import org.simple.clinic.sync.SynceableRepository
import java.util.UUID
import javax.inject.Inject

class ProtocolRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val protocolDao: Protocol.RoomDao,
    private val protocolDrugsDao: ProtocolDrug.RoomDao
) : SynceableRepository<ProtocolDrugsWithDosage, ProtocolPayload> {

  override fun save(records: List<ProtocolDrugsWithDosage>): Completable {
    return Completable.fromAction {
      appDatabase.openHelper.writableDatabase.inTransaction {
        protocolDao.save(records.map { it.protocol })
        protocolDrugsDao.save(records
            .filter { it.drugs.isNotEmpty() }
            .flatMap { it.drugs })
      }
    }
  }

  override fun recordsWithSyncStatus(syncStatus: SyncStatus): Single<List<ProtocolDrugsWithDosage>> {
    return Single.fromCallable {
      emptyList<ProtocolDrugsWithDosage>()
    }
  }

  override fun setSyncStatus(from: SyncStatus, to: SyncStatus): Completable {
    return Completable.fromAction { protocolDao.updateSyncStatus(oldStatus = from, newStatus = to) }
  }

  override fun setSyncStatus(ids: List<UUID>, to: SyncStatus): Completable {
    return Completable.fromAction { protocolDao.updateSyncStatus(uuids = ids, newStatus = to) }
  }

  override fun mergeWithLocalData(payloads: List<ProtocolPayload>): Completable {

    val protocolDrugsWithDosage = payloads
        .filter { payload ->
          val protocolFromDb = protocolDao.getOne(payload.uuid)
          protocolFromDb?.syncStatus.canBeOverriddenByServerCopy()
        }
        .map(::payloadToProtocolDrugWithDosage)

    return save(protocolDrugsWithDosage)
  }

  override fun recordCount(): Observable<Int> {
    return protocolDao.count().toObservable()
  }

  private fun payloadToProtocolDrugWithDosage(payload: ProtocolPayload): ProtocolDrugsWithDosage {
    return ProtocolDrugsWithDosage(
        protocol = payload.toDatabaseModel(newStatus = SyncStatus.DONE),
        drugs = payload.protocolDrugs?.map { it.toDatabaseModel() } ?: emptyList()
    )
  }
}
