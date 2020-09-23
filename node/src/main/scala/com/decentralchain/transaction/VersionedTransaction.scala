package com.decentralchain.transaction

trait VersionedTransaction {
  def version: TxVersion
}
