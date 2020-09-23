package com.decentralchain.it

import com.typesafe.config.ConfigFactory.{defaultApplication, defaultReference}
import com.decentralchain.account.KeyPair
import com.decentralchain.block.Block
import com.decentralchain.common.utils.EitherExt2
import com.decentralchain.consensus.PoSSelector
import com.decentralchain.database.openDB
import com.decentralchain.events.BlockchainUpdateTriggers
import com.decentralchain.history.StorageFactory
import com.decentralchain.settings._
import com.decentralchain.transaction.Asset.unitoken
import com.decentralchain.utils.NTP
import monix.execution.UncaughtExceptionReporter
import monix.reactive.Observer
import net.ceedubs.ficus.Ficus._

object BaseTargetChecker {
  def main(args: Array[String]): Unit = {
    implicit val reporter: UncaughtExceptionReporter = UncaughtExceptionReporter.default
    val sharedConfig = Docker.genesisOverride
      .withFallback(Docker.configTemplate)
      .withFallback(defaultApplication())
      .withFallback(defaultReference())
      .resolve()

    val settings          = unitokenSettings.fromRootConfig(sharedConfig)
    val db                = openDB("/tmp/tmp-db")
    val ntpTime           = new NTP("ntp.pool.org")
    val (blockchainUpdater, _) = StorageFactory(settings, db, ntpTime, Observer.empty, BlockchainUpdateTriggers.noop)
    val poSSelector       = PoSSelector(blockchainUpdater, settings.synchronizationSettings)

    try {
      val genesisBlock = Block.genesis(settings.blockchainSettings.genesisSettings).explicitGet()
      blockchainUpdater.processBlock(genesisBlock, genesisBlock.header.generationSignature)

      NodeConfigs.Default.map(_.withFallback(sharedConfig)).collect {
        case cfg if cfg.as[Boolean]("unitoken.miner.enable") =>
          val account = KeyPair.fromSeed(cfg.getString("account-seed")).explicitGet()
          val address   = account.toAddress
          val balance   = blockchainUpdater.balance(address, unitoken)
          val timeDelay = poSSelector
            .getValidBlockDelay(blockchainUpdater.height, account, genesisBlock.header.baseTarget, balance)
            .explicitGet()

          f"$address: ${timeDelay * 1e-3}%10.3f s"
      }
    } finally ntpTime.close()
  }
}
