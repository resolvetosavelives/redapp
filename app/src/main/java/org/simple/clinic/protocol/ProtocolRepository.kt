package org.simple.clinic.protocol

import androidx.annotation.VisibleForTesting
import io.reactivex.Completable
import io.reactivex.Observable
import org.simple.clinic.AppDatabase
import org.simple.clinic.patient.SyncStatus
import org.simple.clinic.protocol.sync.ProtocolPayload
import org.simple.clinic.storage.inTransaction
import org.simple.clinic.sync.SynceableRepository
import org.simple.clinic.util.UtcClock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class ProtocolRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val utcClock: UtcClock
) : SynceableRepository<ProtocolAndProtocolDrugs, ProtocolPayload> {

  private val protocolDao = appDatabase.protocolDao()
  private val protocolDrugsDao = appDatabase.protocolDrugDao()

  override fun save(records: List<ProtocolAndProtocolDrugs>): Completable {
    return Completable.fromAction { saveRecords(records) }
  }

  private fun saveRecords(records: List<ProtocolAndProtocolDrugs>) {
    appDatabase.openHelper.writableDatabase.inTransaction {
      protocolDao.save(records.map { it.protocol })
      protocolDrugsDao.save(records
          .filter { it.drugs.isNotEmpty() }
          .flatMap { it.drugs })
    }
  }

  fun recordsWithSyncStatus(syncStatus: SyncStatus): List<ProtocolAndProtocolDrugs> {
    val protocolDao = appDatabase.protocolDao()
    val protocolDrugDao = appDatabase.protocolDrugDao()

    val drugsForProtocol = protocolDao
        .withSyncStatus(syncStatus)
        .associateBy({ it }, { protocolDrugDao.drugsForProtocolUuid(it.uuid) })

    return drugsForProtocol.map { (protocol, drugs) -> ProtocolAndProtocolDrugs(protocol, drugs) }
  }

  override fun setSyncStatus(from: SyncStatus, to: SyncStatus) {
    protocolDao.updateSyncStatus(oldStatus = from, newStatus = to)
  }

  override fun setSyncStatus(ids: List<UUID>, to: SyncStatus) {
    protocolDao.updateSyncStatusForIds(uuids = ids, newStatus = to)
  }

  override fun mergeWithLocalData(payloads: List<ProtocolPayload>) {
    val dirtyRecords = appDatabase.protocolDao().recordIdsWithSyncStatus(SyncStatus.PENDING)

    val payloadsToSave = payloads
        .filterNot { it.uuid in dirtyRecords }
        .map(::payloadToProtocolAndDrugs)

    saveRecords(payloadsToSave)
  }

  override fun recordCount(): Observable<Int> {
    return protocolDao.count().toObservable()
  }

  override fun pendingSyncRecordCount(): Observable<Int> {
    return protocolDao.countWithStatus(SyncStatus.PENDING).toObservable()
  }

  override fun pendingSyncRecords(limit: Int, offset: Int): List<ProtocolAndProtocolDrugs> {
    // No implementation needed because this resource is never pushed
    return emptyList()
  }

  private fun payloadToProtocolAndDrugs(payload: ProtocolPayload): ProtocolAndProtocolDrugs {
    return ProtocolAndProtocolDrugs(
        protocol = payload.toDatabaseModel(newStatus = SyncStatus.DONE),
        drugs = payload.protocolDrugs.orEmpty()
            .mapIndexed { index, drug -> drug.toDatabaseModel(index) }
    )
  }

  fun drugsForProtocolOrDefault(protocolUuid: UUID?): List<ProtocolDrugAndDosages> {
    if (protocolUuid == null) {
      return defaultProtocolDrugs()
    }

    val protocolDrugs = protocolDrugsDao
        .drugsForProtocolUuid(protocolUuid)

    return if (protocolDrugs.isEmpty()) {
      defaultProtocolDrugs()
    } else {
      protocolDrugs
          .groupBy { it.name }
          .map { (drugName, protocolDrugs) -> ProtocolDrugAndDosages(drugName, protocolDrugs) }
    }
  }

  fun drugsByNameOrDefault(drugName: String, protocolUuid: UUID?): Observable<List<ProtocolDrug>> {
    if (protocolUuid == null) {
      val defaultDrugs = defaultProtocolDrugs()
          .filter { it.drugName == drugName }
          .flatMap { protocolDrugs -> protocolDrugs.drugs }
      return Observable.just(defaultDrugs)
    }

    return protocolDrugsDao
        .drugByName(drugName, protocolUuid)
        .toObservable()
  }

  fun protocol(protocolUuid: UUID): Observable<Protocol> = protocolDao.protocolStream(protocolUuid)

  fun protocolImmediate(uuid: UUID): Protocol? = protocolDao.getOne(uuid)

  @VisibleForTesting
  fun defaultProtocolDrugs(): List<ProtocolDrugAndDosages> {
    val protocolDrug = { uuid: UUID, name: String, dosage: String, rxNormCode: String? ->
      ProtocolDrug(
          uuid = uuid,
          protocolUuid = UUID.fromString("d486b15b-ac2a-4d2f-b87c-e1905d5ee791"),
          name = name,
          rxNormCode = rxNormCode,
          dosage = dosage,
          createdAt = Instant.now(utcClock),
          updatedAt = Instant.now(utcClock),
          deletedAt = null,
          order = 0
      )
    }
    return listOf(
        ProtocolDrugAndDosages(
            drugName = "Amlodipine",
            drugs = listOf(
                protocolDrug(UUID.fromString("cfe36b5e-fd3f-4f2e-ab95-fee4e1b8a65b"), "Amlodipine", "2.5mg", "329527"),
                protocolDrug(UUID.fromString("66eb8414-423b-46c4-bfe4-dfbd25c61b29"), "Amlodipine", "5mg", "329528"),
                protocolDrug(UUID.fromString("33fcc80d-d791-4529-9998-8eb2e1980e7e"), "Amlodipine", "10mg", "329526")
            )),
        ProtocolDrugAndDosages(
            drugName = "Aspirin",
            drugs = listOf(
                protocolDrug(UUID.fromString("360df3c1-d00f-412a-9c71-bc58285f94e4"), "Aspirin", "75mg", "315429"),
                protocolDrug(UUID.fromString("7a01d999-57d5-407d-a0a6-b74e28d7b3d0"), "Aspirin", "150mg", "315413")
            )),
        ProtocolDrugAndDosages(
            drugName = "Chlorthalidone",
            drugs = listOf(
                protocolDrug(UUID.fromString("d4e4cfc1-c30b-49a6-aede-718c7ef8ea36"), "Chlorthalidone", "12.5mg", "331132"),
                protocolDrug(UUID.fromString("a2fb1e94-e76e-452b-8569-5ba06ac5a418"), "Chlorthalidone", "25mg", "315655")
            )),
        ProtocolDrugAndDosages(
            drugName = "Hydrochlorothiazide",
            drugs = listOf(
                protocolDrug(UUID.fromString("875580d7-74d9-49e7-89f0-4d8d9df40021"), "Hydrochlorothiazide", "12.5mg", "316047"),
                protocolDrug(UUID.fromString("3790002a-3093-4e42-bea2-3c4d893783ba"), "Hydrochlorothiazide", "25mg", "316049"),
                protocolDrug(UUID.fromString("767744d0-0cee-406c-8981-0084334703b1"), "Hydrochlorothiazide", "50mg", "316051")
            )),
        ProtocolDrugAndDosages(
            drugName = "Losartan",
            drugs = listOf(
                protocolDrug(UUID.fromString("320b50ed-d424-47c3-b1a7-4ef6a8e22c80"), "Losartan", "25mg", "979484"),
                protocolDrug(UUID.fromString("959db212-61f2-4bb4-8f0c-1d2300749c51"), "Losartan", "50mg", "979467"),
                protocolDrug(UUID.fromString("06f2934b-5b02-4a6b-ae04-71eef8de02ef"), "Losartan", "100mg", "979463")
            )),
        ProtocolDrugAndDosages(
            drugName = "Losartan-Hydrochlorathlazide",
            drugs = listOf(
                protocolDrug(UUID.fromString("a7e42838-2ade-4012-9b62-ffb838f25a37"), "Losartan-Hydrochlorathiazide", "50mg/12.5mg", "979468"),
                protocolDrug(UUID.fromString("e5d938e5-95c9-4f7e-9aa6-f13845cba19c"), "Losartan-Hydrochlorathiazide", "100mg/25mg", "979471")
            )),
        ProtocolDrugAndDosages(
            drugName = "Telmisartan",
            drugs = listOf(
                protocolDrug(UUID.fromString("e301b7ab-0bf4-4879-a07d-f3641910e3dd"), "Telmisartan", "20mg", "331555"),
                protocolDrug(UUID.fromString("25529619-e224-494c-8098-d353747c0493"), "Telmisartan", "40mg", "316764"),
                protocolDrug(UUID.fromString("f2e15c0e-7d6e-4ca3-9153-596bdf4da839"), "Telmisartan", "80mg", "316765")
            )),
    )
  }
}
