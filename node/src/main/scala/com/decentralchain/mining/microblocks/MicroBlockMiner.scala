package com.decentralchain.mining.microblocks

import com.decentralchain.account.KeyPair
import com.decentralchain.block.Block
import com.decentralchain.mining.{MinerDebugInfo, MiningConstraint}
import com.decentralchain.settings.MinerSettings
import com.decentralchain.state.Blockchain
import com.decentralchain.transaction.BlockchainUpdater
import com.decentralchain.utx.UtxPool
import io.netty.channel.group.ChannelGroup
import monix.eval.Task
import monix.execution.schedulers.SchedulerService

trait MicroBlockMiner {
  def generateMicroBlockSequence(
      account: KeyPair,
      accumulatedBlock: Block,
      restTotalConstraint: MiningConstraint,
      lastMicroBlock: Long
  ): Task[Unit]
}

object MicroBlockMiner {
  def apply(
      setDebugState: MinerDebugInfo.State => Unit,
      allChannels: ChannelGroup,
      blockchainUpdater: BlockchainUpdater with Blockchain,
      utx: UtxPool,
      settings: MinerSettings,
      minerScheduler: SchedulerService,
      appenderScheduler: SchedulerService
  ): MicroBlockMiner =
    new MicroBlockMinerImpl(
      setDebugState,
      allChannels,
      blockchainUpdater,
      utx,
      settings,
      minerScheduler,
      appenderScheduler
    )
}
