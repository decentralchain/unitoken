package com.decentralchain.state.diffs

import com.decentralchain.lang.ValidationError
import com.decentralchain.state.{Diff, LeaseBalance, Portfolio}
import com.decentralchain.transaction.TxValidationError.GenericError
import com.decentralchain.transaction.GenesisTransaction

import scala.util.{Left, Right}

object GenesisTransactionDiff {
  def apply(height: Int)(tx: GenesisTransaction): Either[ValidationError, Diff] = {
    if (height != 1) Left(GenericError(s"GenesisTransaction cannot appear in non-initial block ($height)"))
    else
      Right(Diff(tx = tx, portfolios = Map(tx.recipient -> Portfolio(balance = tx.amount, LeaseBalance.empty, assets = Map.empty))))
  }
}
