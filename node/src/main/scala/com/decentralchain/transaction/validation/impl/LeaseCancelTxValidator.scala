package com.decentralchain.transaction.validation.impl

import cats.data.ValidatedNel
import com.decentralchain.crypto
import com.decentralchain.lang.ValidationError
import com.decentralchain.transaction.TxValidationError.GenericError
import com.decentralchain.transaction.lease.LeaseCancelTransaction
import com.decentralchain.transaction.validation.TxValidator

object LeaseCancelTxValidator extends TxValidator[LeaseCancelTransaction] {
  override def validate(tx: LeaseCancelTransaction): ValidatedNel[ValidationError, LeaseCancelTransaction] = {
    import tx._
    V.seq(tx)(
      V.fee(fee),
      V.cond(leaseId.arr.length == crypto.DigestLength, GenericError("Lease transaction id is invalid"))
    )
  }
}
