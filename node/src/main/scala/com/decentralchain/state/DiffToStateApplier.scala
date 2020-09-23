package com.decentralchain.state

import cats.syntax.monoid._
import com.decentralchain.account.Address
import com.decentralchain.transaction.Asset
import com.decentralchain.transaction.Asset.unitoken

/**
  * A set of functions that apply diff
  * to the blockchain and return new
  * state values (only changed ones)
  */
object DiffToStateApplier {
  case class PortfolioUpdates(
      balances: Map[Address, Map[Asset, Long]],
      leases: Map[Address, LeaseBalance]
  )

  def portfolios(blockchain: Blockchain, diff: Diff): PortfolioUpdates = {
    val balances = Map.newBuilder[Address, Map[Asset, Long]]
    val leases   = Map.newBuilder[Address, LeaseBalance]

    for ((address, portfolioDiff) <- diff.portfolios) {
      // balances for address
      val bs = Map.newBuilder[Asset, Long]

      if (portfolioDiff.balance != 0) {
        bs += unitoken -> (blockchain.balance(address, unitoken) + portfolioDiff.balance)
      }

      portfolioDiff.assets.collect {
        case (asset, balanceDiff) if balanceDiff != 0 =>
          bs += asset -> (blockchain.balance(address, asset) + balanceDiff)
      }

      balances += address -> bs.result()

      // leases
      if (portfolioDiff.lease != LeaseBalance.empty) {
        leases += address -> blockchain.leaseBalance(address).combine(portfolioDiff.lease)
      }
    }

    PortfolioUpdates(balances.result(), leases.result())
  }
}
