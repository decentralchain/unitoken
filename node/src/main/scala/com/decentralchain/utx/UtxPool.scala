package com.decentralchain.utx

import com.decentralchain.account.Address
import com.decentralchain.common.state.ByteStr
import com.decentralchain.lang.ValidationError
import com.decentralchain.mining.MultiDimensionalMiningConstraint
import com.decentralchain.state.Portfolio
import com.decentralchain.transaction._
import com.decentralchain.transaction.smart.script.trace.TracedResult
import com.decentralchain.utx.UtxPool.PackStrategy

import scala.concurrent.duration.FiniteDuration

trait UtxPool extends AutoCloseable {
  def putIfNew(tx: Transaction, forceValidate: Boolean = false): TracedResult[ValidationError, Boolean]
  def removeAll(txs: Iterable[Transaction]): Unit
  def spendableBalance(addr: Address, assetId: Asset): Long
  def pessimisticPortfolio(addr: Address): Portfolio
  def all: Seq[Transaction]
  def size: Int
  def transactionById(transactionId: ByteStr): Option[Transaction]
  def packUnconfirmed(
      rest: MultiDimensionalMiningConstraint,
      strategy: PackStrategy = PackStrategy.Unlimited,
      cancelled: () => Boolean = () => false
  ): (Option[Seq[Transaction]], MultiDimensionalMiningConstraint)
  def nextMicroBlockSize(): Option[Int]
}

object UtxPool {
  sealed trait PackStrategy
  object PackStrategy {
    case class Limit(time: FiniteDuration)    extends PackStrategy
    case class Estimate(time: FiniteDuration) extends PackStrategy
    case object Unlimited                     extends PackStrategy
  }
}
