package com.decentralchain.transaction
import com.decentralchain.account.PublicKey

trait Authorized {
  val sender: PublicKey
}
