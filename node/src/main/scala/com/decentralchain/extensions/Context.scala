package com.decentralchain.extensions

import akka.actor.ActorSystem
import com.decentralchain.account.Address
import com.decentralchain.api.common._
import com.decentralchain.common.state.ByteStr
import com.decentralchain.events.{BlockchainUpdated, UtxEvent}
import com.decentralchain.lang.ValidationError
import com.decentralchain.settings.unitokenSettings
import com.decentralchain.state.Blockchain
import com.decentralchain.transaction.smart.script.trace.TracedResult
import com.decentralchain.transaction.{Asset, DiscardedBlocks, Transaction}
import com.decentralchain.utils.Time
import com.decentralchain.utx.UtxPool
import com.decentralchain.wallet.Wallet
import monix.eval.Task
import monix.reactive.Observable

trait Context {
  def settings: unitokenSettings
  def blockchain: Blockchain
  def rollbackTo(blockId: ByteStr): Task[Either[ValidationError, DiscardedBlocks]]
  def time: Time
  def wallet: Wallet
  def utx: UtxPool

  def transactionsApi: CommonTransactionsApi
  def blocksApi: CommonBlocksApi
  def accountsApi: CommonAccountsApi
  def assetsApi: CommonAssetsApi

  def broadcastTransaction(tx: Transaction): TracedResult[ValidationError, Boolean]
  def spendableBalanceChanged: Observable[(Address, Asset)]
  def blockchainUpdated: Observable[BlockchainUpdated]
  def utxEvents: Observable[UtxEvent]
  def actorSystem: ActorSystem
}
