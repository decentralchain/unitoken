package com.decentralchain.database

import com.google.common.hash.{Funnels, BloomFilter => GBloomFilter}
import com.decentralchain.account.Address
import com.decentralchain.events.BlockchainUpdateTriggers
import com.decentralchain.settings.unitokenSettings
import com.decentralchain.state.BlockchainUpdaterImpl
import com.decentralchain.transaction.Asset
import com.decentralchain.utils.Time
import monix.reactive.Observer
import org.iq80.leveldb.DB

object TestStorageFactory {
  private def wrappedFilter(use: Boolean): BloomFilter =
    if (use) new Wrapper(GBloomFilter.create(Funnels.byteArrayFunnel(), 1000L)) else BloomFilter.AlwaysEmpty

  def apply(
      settings: unitokenSettings,
      db: DB,
      time: Time,
      spendableBalanceChanged: Observer[(Address, Asset)],
      blockchainUpdateTriggers: BlockchainUpdateTriggers
  ): (BlockchainUpdaterImpl, LevelDBWriter) = {
    val useBloomFilter = settings.dbSettings.useBloomFilter
    val levelDBWriter: LevelDBWriter = new LevelDBWriter(db, spendableBalanceChanged, settings.blockchainSettings, settings.dbSettings) {
      override val orderFilter: BloomFilter        = wrappedFilter(useBloomFilter)
      override val dataKeyFilter: BloomFilter      = wrappedFilter(useBloomFilter)
      override val unitokenBalanceFilter: BloomFilter = wrappedFilter(useBloomFilter)
      override val assetBalanceFilter: BloomFilter = wrappedFilter(useBloomFilter)
    }
    (
      new BlockchainUpdaterImpl(levelDBWriter, spendableBalanceChanged, settings, time, blockchainUpdateTriggers, loadActiveLeases(db, _, _)),
      levelDBWriter
    )
  }
}
