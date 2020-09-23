package com.decentralchain.api.http.requests

import com.decentralchain.account.PublicKey
import com.decentralchain.lang.ValidationError
import com.decentralchain.transaction.Transaction
import com.decentralchain.transaction.TxValidationError.GenericError

trait TxBroadcastRequest {
  def sender: Option[String]
  def senderPublicKey: Option[String]

  def toTxFrom(sender: PublicKey): Either[ValidationError, Transaction]

  def toTx: Either[ValidationError, Transaction] =
    for {
      sender <- senderPublicKey match {
        case Some(key) => PublicKey.fromBase58String(key)
        case None      => Left(GenericError("invalid.senderPublicKey"))
      }
      tx <- toTxFrom(sender)
    } yield tx
}
