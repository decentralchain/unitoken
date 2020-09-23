package com.decentralchain.settings

import com.typesafe.config.{Config, ConfigFactory}
import com.decentralchain.metrics.Metrics
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import scala.concurrent.duration.FiniteDuration

case class unitokenSettings(
    directory: String,
    ntpServer: String,
    dbSettings: DBSettings,
    extensions: Seq[String],
    extensionsShutdownTimeout: FiniteDuration,
    networkSettings: NetworkSettings,
    walletSettings: WalletSettings,
    blockchainSettings: BlockchainSettings,
    minerSettings: MinerSettings,
    restAPISettings: RestAPISettings,
    synchronizationSettings: SynchronizationSettings,
    utxSettings: UtxSettings,
    featuresSettings: FeaturesSettings,
    rewardsSettings: RewardsVotingSettings,
    metrics: Metrics.Settings,
    config: Config
)

object unitokenSettings extends CustomValueReaders {
  def fromRootConfig(rootConfig: Config): unitokenSettings = {
    val unitoken = rootConfig.getConfig("unitoken")

    val directory                 = unitoken.as[String]("directory")
    val ntpServer                 = unitoken.as[String]("ntp-server")
    val dbSettings                = unitoken.as[DBSettings]("db")
    val extensions                = unitoken.as[Seq[String]]("extensions")
    val extensionsShutdownTimeout = unitoken.as[FiniteDuration]("extensions-shutdown-timeout")
    val networkSettings           = unitoken.as[NetworkSettings]("network")
    val walletSettings            = unitoken.as[WalletSettings]("wallet")
    val blockchainSettings        = unitoken.as[BlockchainSettings]("blockchain")
    val minerSettings             = unitoken.as[MinerSettings]("miner")
    val restAPISettings           = unitoken.as[RestAPISettings]("rest-api")
    val synchronizationSettings   = unitoken.as[SynchronizationSettings]("synchronization")
    val utxSettings               = unitoken.as[UtxSettings]("utx")
    val featuresSettings          = unitoken.as[FeaturesSettings]("features")
    val rewardsSettings           = unitoken.as[RewardsVotingSettings]("rewards")
    val metrics                   = rootConfig.as[Metrics.Settings]("metrics") // TODO: Move to unitoken section

    unitokenSettings(
      directory,
      ntpServer,
      dbSettings,
      extensions,
      extensionsShutdownTimeout,
      networkSettings,
      walletSettings,
      blockchainSettings,
      minerSettings,
      restAPISettings,
      synchronizationSettings,
      utxSettings,
      featuresSettings,
      rewardsSettings,
      metrics,
      rootConfig
    )
  }

  def default(): unitokenSettings = fromRootConfig(ConfigFactory.load())
}
