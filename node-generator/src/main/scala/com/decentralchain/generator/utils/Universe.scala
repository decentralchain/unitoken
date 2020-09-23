package com.decentralchain.generator.utils

import com.decentralchain.generator.Preconditions.CreatedAccount
import com.decentralchain.transaction.assets.IssueTransaction
import com.decentralchain.transaction.lease.LeaseTransaction

object Universe {
  @volatile var Accounts: List[CreatedAccount]       = Nil
  @volatile var IssuedAssets: List[IssueTransaction] = Nil
  @volatile var Leases: List[LeaseTransaction]       = Nil
}
