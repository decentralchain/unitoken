package com.decentralchain.api
import com.decentralchain.account.Address
import com.decentralchain.common.state.ByteStr
import com.decentralchain.database.{DBExt, Keys}
import com.decentralchain.state.{Diff, Height}
import com.decentralchain.transaction.CreateAliasTransaction
import com.decentralchain.transaction.lease.LeaseTransaction
import monix.reactive.Observable
import org.iq80.leveldb.DB

package object common extends BalanceDistribution with AddressTransactions {
  def aliasesOfAddress(db: DB, maybeDiff: => Option[(Height, Diff)], address: Address): Observable[(Height, CreateAliasTransaction)] = {
    val disabledAliases = db.get(Keys.disabledAliases)
    addressTransactions(db, maybeDiff, address, Some(address), Set(CreateAliasTransaction.typeId), None)
      .collect { case (height, cat: CreateAliasTransaction, true) if disabledAliases.isEmpty || !disabledAliases(cat.alias) => height -> cat }
  }

  def activeLeases(
      db: DB,
      maybeDiff: Option[(Height, Diff)],
      address: Address,
      leaseIsActive: ByteStr => Boolean
  ): Observable[(Height, LeaseTransaction)] =
    addressTransactions(db, maybeDiff, address, None, Set(LeaseTransaction.typeId), None)
      .collect { case (h, lt: LeaseTransaction, true) if leaseIsActive(lt.id()) => h -> lt }
}
