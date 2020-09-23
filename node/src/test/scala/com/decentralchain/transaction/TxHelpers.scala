package com.decentralchain.transaction

import com.google.common.primitives.Ints
import com.decentralchain.TestValues
import com.decentralchain.account.{Address, AddressOrAlias, KeyPair}
import com.decentralchain.common.state.ByteStr
import com.decentralchain.common.utils._
import com.decentralchain.it.util.DoubleExt
import com.decentralchain.lang.script.Script
import com.decentralchain.lang.v1.FunctionHeader
import com.decentralchain.lang.v1.compiler.Terms.{EXPR, FUNCTION_CALL}
import com.decentralchain.transaction.Asset.unitoken
import com.decentralchain.transaction.assets.IssueTransaction
import com.decentralchain.transaction.assets.exchange.{AssetPair, ExchangeTransaction, Order, OrderType}
import com.decentralchain.transaction.smart.InvokeScriptTransaction.Payment
import com.decentralchain.transaction.smart.{InvokeScriptTransaction, SetScriptTransaction}
import com.decentralchain.transaction.transfer.TransferTransaction

object TxHelpers {
  def signer(i: Int): KeyPair = KeyPair(Ints.toByteArray(i))
  val defaultSigner: KeyPair  = signer(0)

  private[this] var lastTimestamp = System.currentTimeMillis()
  def timestamp: Long = {
    lastTimestamp += 1
    lastTimestamp
  }

  def genesis(address: Address, amount: Long = 1000.unitoken): GenesisTransaction =
    GenesisTransaction.create(address, amount, timestamp).explicitGet()

  def transfer(from: KeyPair, to: AddressOrAlias, amount: Long = 1.unitoken, asset: Asset = unitoken): TransferTransaction =
    TransferTransaction.selfSigned(TxVersion.V1, from, to, asset, amount, unitoken, TestValues.fee, ByteStr.empty, timestamp).explicitGet()

  def issue(amount: Long = 1000, script: Script = null): IssueTransaction =
    IssueTransaction
      .selfSigned(TxVersion.V2, defaultSigner, "test", "", amount, 0, reissuable = true, Option(script), 1.unitoken, timestamp)
      .explicitGet()

  def orderV3(orderType: OrderType, asset: Asset, feeAsset: Asset): Order = {
    orderV3(orderType, asset, unitoken, feeAsset)
  }

  def order(orderType: OrderType, asset: Asset): Order =
    orderV3(orderType, asset, unitoken)

  def orderV3(orderType: OrderType, amountAsset: Asset, priceAsset: Asset, feeAsset: Asset): Order = {
    Order.selfSigned(
      TxVersion.V3,
      defaultSigner,
      defaultSigner.publicKey,
      AssetPair(amountAsset, priceAsset),
      orderType,
      1L,
      1L,
      timestamp,
      timestamp + 100000,
      1L,
      feeAsset
    )
  }

  def exchange(order1: Order, order2: Order): ExchangeTransaction = {
    ExchangeTransaction
      .signed(
        TxVersion.V2,
        defaultSigner.privateKey,
        order1,
        order2,
        order1.amount,
        order1.price,
        order1.matcherFee,
        order2.matcherFee,
        TestValues.fee,
        timestamp
      )
      .explicitGet()
  }

  def setScript(acc: KeyPair, script: Script): SetScriptTransaction = {
    SetScriptTransaction.selfSigned(TxVersion.V1, acc, Some(script), TestValues.fee, timestamp).explicitGet()
  }

  def invoke(dApp: AddressOrAlias, func: String, args: Seq[EXPR] = Nil, payments: Seq[Payment] = Nil): InvokeScriptTransaction = {
    val fc = FUNCTION_CALL(FunctionHeader.User(func), args.toList)
    InvokeScriptTransaction.selfSigned(TxVersion.V1, defaultSigner, dApp, Some(fc), payments, TestValues.fee, Asset.unitoken, timestamp).explicitGet()
  }
}
