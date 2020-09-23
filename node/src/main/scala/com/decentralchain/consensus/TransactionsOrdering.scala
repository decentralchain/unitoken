package com.decentralchain.consensus

import com.decentralchain.transaction.Asset.unitoken
import com.decentralchain.transaction.Transaction

object TransactionsOrdering {
  trait unitokenOrdering extends Ordering[Transaction] {
    def txTimestampOrder(ts: Long): Long
    private def orderBy(t: Transaction): (Double, Long, Long) = {
      val size        = t.bytes().length
      val byFee       = if (t.assetFee._1 != unitoken) 0 else -t.assetFee._2
      val byTimestamp = txTimestampOrder(t.timestamp)

      (byFee.toDouble / size.toDouble, byFee, byTimestamp)
    }
    override def compare(first: Transaction, second: Transaction): Int = {
      import Ordering.Double.TotalOrdering
      implicitly[Ordering[(Double, Long, Long)]].compare(orderBy(first), orderBy(second))
    }
  }

  object InBlock extends unitokenOrdering {
    // sorting from network start
    override def txTimestampOrder(ts: Long): Long = -ts
  }

  object InUTXPool extends unitokenOrdering {
    override def txTimestampOrder(ts: Long): Long = ts
  }
}
