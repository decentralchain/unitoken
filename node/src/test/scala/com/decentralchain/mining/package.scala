package com.decentralchain

import com.decentralchain.state.{Blockchain, Diff}
import com.decentralchain.transaction.Transaction

package object mining {
  private[mining] def createConstConstraint(maxSize: Long, transactionSize: => Long, description: String) = OneDimensionalMiningConstraint(
    maxSize,
    new com.decentralchain.mining.TxEstimators.Fn {
      override def apply(b: Blockchain, t: Transaction, d: Diff): Long = transactionSize
      override val minEstimate                                         = transactionSize
      override val toString: String                                    = s"const($transactionSize)"
    },
    description
  )
}
