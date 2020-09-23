package com.decentralchain.transaction.validation.impl

import cats.data.ValidatedNel
import com.decentralchain.lang.ValidationError
import com.decentralchain.transaction.PaymentTransaction
import com.decentralchain.transaction.validation.TxValidator

object PaymentTxValidator extends TxValidator[PaymentTransaction] {
  override def validate(transaction: PaymentTransaction): ValidatedNel[ValidationError, PaymentTransaction] = {
    import transaction._
    V.seq(transaction)(
      V.fee(fee),
      V.positiveAmount(amount, "unitoken"),
      V.noOverflow(fee, amount),
      V.addressChainId(recipient, chainId)
    )
  }
}
