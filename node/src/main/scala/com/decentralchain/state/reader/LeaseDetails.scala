package com.decentralchain.state.reader

import com.decentralchain.account.{PublicKey, AddressOrAlias}

case class LeaseDetails(sender: PublicKey, recipient: AddressOrAlias, height: Int, amount: Long, isActive: Boolean)
