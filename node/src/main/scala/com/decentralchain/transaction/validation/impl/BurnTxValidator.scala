package com.decentralchain.transaction.validation.impl

import com.decentralchain.transaction.assets.BurnTransaction
import com.decentralchain.transaction.validation.{TxValidator, ValidatedV}

object BurnTxValidator extends TxValidator[BurnTransaction] {
  override def validate(tx: BurnTransaction): ValidatedV[BurnTransaction] = {
    import tx._
    V.seq(tx)(
      V.positiveOrZeroAmount(quantity, "assets"),
      V.fee(fee)
    )
  }
}
