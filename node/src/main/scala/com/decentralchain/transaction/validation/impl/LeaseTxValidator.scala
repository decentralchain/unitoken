package com.decentralchain.transaction.validation.impl

import cats.data.ValidatedNel
import com.decentralchain.lang.ValidationError
import com.decentralchain.transaction.TxValidationError
import com.decentralchain.transaction.lease.LeaseTransaction
import com.decentralchain.transaction.validation.TxValidator

object LeaseTxValidator extends TxValidator[LeaseTransaction] {
  override def validate(tx: LeaseTransaction): ValidatedNel[ValidationError, LeaseTransaction] = {
    import tx._
    V.seq(tx)(
      V.fee(fee),
      V.cond(amount > 0, TxValidationError.NonPositiveAmount(amount, "unitoken")),
      V.noOverflow(amount, fee),
      V.cond(sender.toAddress != recipient, TxValidationError.ToSelf),
      V.addressChainId(recipient, chainId)
    )
  }
}
