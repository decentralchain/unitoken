package com.decentralchain.transaction.validation.impl

import com.decentralchain.transaction.smart.SetScriptTransaction
import com.decentralchain.transaction.validation.{TxValidator, _}

object SetScriptTxValidator extends TxValidator[SetScriptTransaction] {
  override def validate(tx: SetScriptTransaction): ValidatedV[SetScriptTransaction] =
    V.fee(tx.fee).map(_ => tx)
}
