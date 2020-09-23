package com.decentralchain.http
import com.decentralchain.lang.ValidationError
import com.decentralchain.network.UtxPoolSynchronizer
import com.decentralchain.transaction.Transaction
import com.decentralchain.transaction.smart.script.trace.TracedResult
import io.netty.channel.Channel

object DummyUtxPoolSynchronizer {
  val accepting: UtxPoolSynchronizer = new UtxPoolSynchronizer {
    override def tryPublish(tx: Transaction, source: Channel): Unit               = {}
    override def publish(tx: Transaction): TracedResult[ValidationError, Boolean] = TracedResult(Right(true))
  }

  def rejecting(error: Transaction => ValidationError): UtxPoolSynchronizer = new UtxPoolSynchronizer {
    override def tryPublish(tx: Transaction, source: Channel): Unit               = {}
    override def publish(tx: Transaction): TracedResult[ValidationError, Boolean] = TracedResult(Left(error(tx)))
  }
}
