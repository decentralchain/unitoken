package com.decentralchain.state.diffs

import com.decentralchain.features.BlockchainFeatures
import com.decentralchain.lang.ValidationError
import com.decentralchain.state.{Blockchain, Diff, LeaseBalance, Portfolio}
import com.decentralchain.transaction.CreateAliasTransaction
import com.decentralchain.transaction.TxValidationError.GenericError

import scala.util.Right

object CreateAliasTransactionDiff {
  def apply(blockchain: Blockchain)(tx: CreateAliasTransaction): Either[ValidationError, Diff] =
    if (blockchain.isFeatureActivated(BlockchainFeatures.DataTransaction, blockchain.height) && !blockchain.canCreateAlias(tx.alias))
      Left(GenericError("Alias already claimed"))
    else
      Right(
        Diff(
          tx = tx,
          portfolios = Map(tx.sender.toAddress -> Portfolio(-tx.fee, LeaseBalance.empty, Map.empty)),
          aliases = Map(tx.alias               -> tx.sender.toAddress),
          scriptsRun = DiffsCommon.countScriptRuns(blockchain, tx)
        )
      )
}
