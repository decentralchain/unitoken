package com.decentralchain.events

import java.nio.file.Files

import com.decentralchain.database.TestStorageFactory
import com.decentralchain.settings.{unitokenSettings, loadConfig}
import com.decentralchain.state.Blockchain
import com.decentralchain.transaction.BlockchainUpdater
import com.decentralchain.{NTPTime, TestHelpers, database}
import monix.reactive.Observer
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait WithBlockchain extends BeforeAndAfterEach with BeforeAndAfterAll with NTPTime { _: Suite =>
  protected def settings: unitokenSettings = unitokenSettings.fromRootConfig(loadConfig(None))

  private val path = Files.createTempDirectory("leveldb-test")
  private val db   = database.openDB(path.toAbsolutePath.toString)
  private val (bcu, _) = TestStorageFactory(
    settings,
    db,
    ntpTime,
    Observer.stopped,
    BlockchainUpdateTriggers.noop
  )

  protected def blockchain: Blockchain = bcu

  /**
    * Override this method to do some initialization actions with
    * the blockchain before it becomes read-only
    * @param blockchainUpdater a blockchain to add something to (genesis, some blocks, etc.)
    */
  protected def initBlockchain(blockchainUpdater: Blockchain with BlockchainUpdater): Unit = ()

  override protected def beforeAll(): Unit = {
    initBlockchain(bcu)
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    bcu.shutdown()
    db.close()
    TestHelpers.deleteRecursively(path)
    super.afterAll()
  }
}
