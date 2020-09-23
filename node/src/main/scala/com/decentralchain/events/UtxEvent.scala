package com.decentralchain.events

import com.decentralchain.lang.ValidationError
import com.decentralchain.state.Diff
import com.decentralchain.transaction.Transaction

sealed trait UtxEvent
object UtxEvent {
  final case class TxAdded(tx: Transaction, diff: Diff) extends UtxEvent {
    override def toString: String = s"TxAdded(${tx.id()})"
  }
  final case class TxRemoved(tx: Transaction, reason: Option[ValidationError]) extends UtxEvent {
    override def toString: String = s"TxRemoved(${tx.id()})"
  }
}
