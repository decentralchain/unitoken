package com.wavesplatform.events

import cats.syntax.monoid._
import com.wavesplatform.account.Address
import com.wavesplatform.block.{Block, MicroBlock}
import com.wavesplatform.common.state.ByteStr
import com.wavesplatform.state.DiffToStateApplier.PortfolioUpdates
import com.wavesplatform.state.diffs.BlockDiffer.DetailedDiff
import com.wavesplatform.state.reader.CompositeBlockchain
import com.wavesplatform.state.{AccountDataInfo, AssetDescription, AssetScriptInfo, Blockchain, DataEntry, Diff, DiffToStateApplier, LeaseBalance}
import com.wavesplatform.transaction.{Asset, GenesisTransaction, Transaction}
import com.wavesplatform.transaction.Asset.IssuedAsset

import scala.collection.mutable.ArrayBuffer

final case class AssetStateUpdate(
    asset: IssuedAsset,
    decimals: Int,
    name: ByteStr,
    description: ByteStr,
    reissuable: Boolean,
    volume: BigInt,
    script: Option[AssetScriptInfo],
    sponsorship: Option[Long],
    nft: Boolean,
    assetExistedBefore: Boolean
)

final case class StateUpdate(
    balances: Seq[(Address, Asset, Long)],
    leases: Seq[(Address, LeaseBalance)],
    dataEntries: Seq[(Address, DataEntry[_])],
    assets: Seq[AssetStateUpdate]
) {
  def isEmpty: Boolean = balances.isEmpty && leases.isEmpty && dataEntries.isEmpty && assets.isEmpty
}

object StateUpdate {
  def atomic(blockchainBefore: Blockchain, diff: Diff, byTransaction: Option[Transaction]): StateUpdate = {
    val blockchainAfter = CompositeBlockchain(blockchainBefore, Some(diff))

    val PortfolioUpdates(updatedBalances, updatedLeases) = DiffToStateApplier.portfolios(blockchainBefore, diff)

    val balances = ArrayBuffer.empty[(Address, Asset, Long)]
    for ((address, assetMap) <- updatedBalances; (asset, balance) <- assetMap) balances += ((address, asset, balance))

    val dataEntries = diff.accountData.toSeq.flatMap {
      case (address, AccountDataInfo(data)) =>
        data.toSeq.map { case (_, entry) => (address, entry) }
    }

    val assets: Seq[AssetStateUpdate] = for {
      a <- (diff.issuedAssets.keySet ++ diff.updatedAssets.keySet ++ diff.assetScripts.keySet ++ diff.sponsorship.keySet).toSeq
      AssetDescription(
        _,
        _,
        name,
        description,
        decimals,
        reissuable,
        totalVolume,
        _,
        script,
        sponsorship,
        nft
      ) <- blockchainAfter.assetDescription(a).toSeq
      existedBefore = !diff.issuedAssets.isDefinedAt(a)
    } yield AssetStateUpdate(
      a,
      decimals,
      ByteStr(name.toByteArray),
      ByteStr(description.toByteArray),
      reissuable,
      totalVolume,
      script,
      if (sponsorship == 0) None else Some(sponsorship),
      nft,
      existedBefore
    )

    StateUpdate(balances.toSeq, updatedLeases.toSeq, dataEntries, assets)
  }

  def container(
      blockchainBefore: Blockchain,
      diff: DetailedDiff,
      transactions: Seq[Transaction]
  ): (StateUpdate, Seq[StateUpdate]) = {
    val DetailedDiff(parentDiff, txsDiffs) = diff
    val parentStateUpdate                  = atomic(blockchainBefore, parentDiff, None)

    val (txsStateUpdates, _) = txsDiffs
      .zip(transactions)
      .foldLeft((ArrayBuffer.empty[StateUpdate], parentDiff)) {
        case ((updates, accDiff), (txDiff, tx)) =>
          (
            updates += atomic(CompositeBlockchain(blockchainBefore, Some(accDiff)), txDiff, Some(tx)),
            accDiff.combine(txDiff)
          )
      }

    (parentStateUpdate, txsStateUpdates.toSeq)
  }
}

sealed trait BlockchainUpdated extends Product with Serializable {
  def toId: ByteStr
  def toHeight: Int
}

final case class BlockAppended(
    toId: ByteStr,
    toHeight: Int,
    block: Block,
    updatedWavesAmount: Long,
    blockStateUpdate: StateUpdate,
    transactionStateUpdates: Seq[StateUpdate]
) extends BlockchainUpdated

object BlockAppended {
  def from(block: Block, diff: DetailedDiff, minerReward: Option[Long], blockchainBefore: Blockchain): BlockAppended = {
    val (blockStateUpdate, txsStateUpdates) = StateUpdate.container(blockchainBefore, diff, block.transactionData)

    // updatedWavesAmount can change as a result of either genesis transactions or miner rewards
    val updatedWavesAmount = blockchainBefore.height match {
      // genesis case
      case 0 => block.transactionData.collect { case GenesisTransaction(_, amount, _, _, _) => amount }.sum
      // miner reward case
      case _ => blockchainBefore.wavesAmount(blockchainBefore.height).toLong + minerReward.getOrElse(0L)
    }

    BlockAppended(block.signature, blockchainBefore.height + 1, block, updatedWavesAmount, blockStateUpdate, txsStateUpdates)
  }
}

final case class MicroBlockAppended(
    toId: ByteStr,
    toHeight: Int,
    microBlock: MicroBlock,
    microBlockStateUpdate: StateUpdate,
    transactionStateUpdates: Seq[StateUpdate]
) extends BlockchainUpdated

object MicroBlockAppended {
  def from(microBlock: MicroBlock, diff: DetailedDiff, blockchainBefore: Blockchain, totalBlockId: ByteStr): MicroBlockAppended = {
    val (microBlockStateUpdate, txsStateUpdates) = StateUpdate.container(blockchainBefore, diff, microBlock.transactionData)
    MicroBlockAppended(totalBlockId, blockchainBefore.height, microBlock, microBlockStateUpdate, txsStateUpdates)
  }
}

final case class RollbackCompleted(toId: ByteStr, toHeight: Int) extends BlockchainUpdated

object RollbackCompleted {
  def from(toBlockId: ByteStr, toHeight: Int): RollbackCompleted = RollbackCompleted(toBlockId, toHeight)
}

final case class MicroBlockRollbackCompleted(toId: ByteStr, toHeight: Int) extends BlockchainUpdated

object MicroBlockRollbackCompleted {
  def from(toBlockId: ByteStr, height: Int): MicroBlockRollbackCompleted = MicroBlockRollbackCompleted(toBlockId, height)
}
