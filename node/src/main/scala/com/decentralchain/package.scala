package com

import com.decentralchain.block.Block
import com.decentralchain.common.state.ByteStr
import com.decentralchain.lang.ValidationError
import com.decentralchain.settings.unitokenSettings
import com.decentralchain.state.Blockchain
import com.decentralchain.transaction.BlockchainUpdater
import com.decentralchain.transaction.TxValidationError.GenericError
import com.decentralchain.utils.ScorexLogging

package object decentralchain extends ScorexLogging {
  private def checkOrAppend(block: Block, blockchainUpdater: Blockchain with BlockchainUpdater): Either[ValidationError, Unit] = {
    if (blockchainUpdater.isEmpty) {
      blockchainUpdater.processBlock(block, block.header.generationSignature).map { _ =>
        log.info(s"Genesis block ${blockchainUpdater.blockHeader(1).get.header} has been added to the state")
      }
    } else {
      val existingGenesisBlockId: Option[ByteStr] = blockchainUpdater.blockHeader(1).map(_.id())
      Either.cond(existingGenesisBlockId.fold(false)(_ == block.id()),
                  (),
                  GenericError("Mismatched genesis blocks in configuration and blockchain"))
    }
  }

  def checkGenesis(settings: unitokenSettings, blockchainUpdater: Blockchain with BlockchainUpdater): Unit = {
    Block
      .genesis(settings.blockchainSettings.genesisSettings)
      .flatMap { genesis =>
        log.trace(s"Genesis block json: ${genesis.json()}")
        checkOrAppend(genesis, blockchainUpdater)
      }
      .left
      .foreach { e =>
        log.error("INCORRECT NODE CONFIGURATION!!! NODE STOPPED BECAUSE OF THE FOLLOWING ERROR:")
        log.error(e.toString)
        com.decentralchain.utils.forceStopApplication()
      }
  }
}
