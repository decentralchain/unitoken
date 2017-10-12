package com.wavesplatform.settings

import com.typesafe.config.Config
import com.wavesplatform.settings.SynchronizationSettings.{HistoryReplierSettings, MicroblockSynchronizerSettings}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import scala.concurrent.duration.FiniteDuration

case class SynchronizationSettings(maxRollback: Int,
                                   maxChainLength: Int,
                                   synchronizationTimeout: FiniteDuration,
                                   scoreTTL: FiniteDuration,
                                   microBlockSynchronizer: MicroblockSynchronizerSettings,
                                   historyReplierSettings: HistoryReplierSettings)

object SynchronizationSettings {

  case class MicroblockSynchronizerSettings(waitResponseTimeout: FiniteDuration,
                                            processedMicroBlocksCacheTimeout: FiniteDuration,
                                            invCacheTimeout: FiniteDuration,
                                            nextInvCacheTimeout: FiniteDuration)

  case class HistoryReplierSettings(maxMicroBlockCacheSize: Int,
                                    maxBlockCacheSize: Int)

  val configPath: String = "waves.synchronization"

  def fromConfig(config: Config): SynchronizationSettings = {
    val maxRollback = config.as[Int](s"$configPath.max-rollback")
    val maxChainLength = config.as[Int](s"$configPath.max-chain-length")
    val synchronizationTimeout = config.as[FiniteDuration](s"$configPath.synchronization-timeout")
    val scoreTTL = config.as[FiniteDuration](s"$configPath.score-ttl")
    val microBlockSynchronizer = config.as[MicroblockSynchronizerSettings](s"$configPath.micro-block-synchronizer")
    val historyReplierSettings = config.as[HistoryReplierSettings](s"$configPath.history-replier")

    SynchronizationSettings(maxRollback, maxChainLength, synchronizationTimeout, scoreTTL, microBlockSynchronizer, historyReplierSettings)
  }
}