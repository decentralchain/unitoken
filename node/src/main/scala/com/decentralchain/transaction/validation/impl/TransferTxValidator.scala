package com.decentralchain.transaction.validation.impl

import cats.data.ValidatedNel
import com.decentralchain.lang.ValidationError
import com.decentralchain.transaction.transfer.TransferTransaction
import com.decentralchain.transaction.validation.TxValidator

object TransferTxValidator extends TxValidator[TransferTransaction] {
  override def validate(transaction: TransferTransaction): ValidatedNel[ValidationError, TransferTransaction] = {
    import transaction._
    V.seq(transaction)(
      V.fee(fee),
      V.positiveAmount(amount, assetId.maybeBase58Repr.getOrElse("unitoken")),
      V.transferAttachment(attachment),
      V.addressChainId(recipient, chainId)
    )
  }
}
