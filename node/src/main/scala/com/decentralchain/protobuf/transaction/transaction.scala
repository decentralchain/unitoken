package com.decentralchain.protobuf

package object transaction {
  type PBOrder = com.decentralchain.protobuf.order.Order
  val PBOrder = com.decentralchain.protobuf.order.Order

  type VanillaOrder = com.decentralchain.transaction.assets.exchange.Order
  val VanillaOrder = com.decentralchain.transaction.assets.exchange.Order

  type PBTransaction = com.decentralchain.protobuf.transaction.Transaction
  val PBTransaction = com.decentralchain.protobuf.transaction.Transaction

  type PBSignedTransaction = com.decentralchain.protobuf.transaction.SignedTransaction
  val PBSignedTransaction = com.decentralchain.protobuf.transaction.SignedTransaction

  type VanillaTransaction = com.decentralchain.transaction.Transaction
  val VanillaTransaction = com.decentralchain.transaction.Transaction

  type VanillaSignedTransaction = com.decentralchain.transaction.SignedTransaction

  type VanillaAssetId = com.decentralchain.transaction.Asset
}
