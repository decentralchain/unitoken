package com.decentralchain.state.diffs

import cats.implicits._
import com.decentralchain.account.Address
import com.decentralchain.lang.ValidationError
import com.decentralchain.state.{Blockchain, Diff, LeaseBalance, Portfolio}
import com.decentralchain.transaction.PaymentTransaction
import com.decentralchain.transaction.TxValidationError.GenericError

import scala.util.{Left, Right}

object PaymentTransactionDiff {

  def apply(blockchain: Blockchain)(tx: PaymentTransaction): Either[ValidationError, Diff] = {
    val blockVersion3AfterHeight = blockchain.settings.functionalitySettings.blockVersion3AfterHeight
    if (blockchain.height > blockVersion3AfterHeight) {
      Left(GenericError(s"Payment transaction is deprecated after h=$blockVersion3AfterHeight"))
    } else {
      Right(
        Diff(
          tx = tx,
          portfolios = Map(tx.recipient -> Portfolio(balance = tx.amount, LeaseBalance.empty, assets = Map.empty)) combine Map(
            Address.fromPublicKey(tx.sender) -> Portfolio(
              balance = -tx.amount - tx.fee,
              LeaseBalance.empty,
              assets = Map.empty
            ))
        ))
    }
  }
}
