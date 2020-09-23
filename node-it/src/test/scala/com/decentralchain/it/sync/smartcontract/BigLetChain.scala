package com.decentralchain.it.sync.smartcontract

import com.decentralchain.common.state.ByteStr
import com.decentralchain.common.utils.EitherExt2
import com.decentralchain.it.api.SyncHttpApi._
import com.decentralchain.it.sync._
import com.decentralchain.it.transactions.BaseTransactionSuite
import com.decentralchain.it.util._
import com.decentralchain.lang.v1.estimator.v2.ScriptEstimatorV2
import com.decentralchain.transaction.Asset.unitoken
import com.decentralchain.transaction.smart.SetScriptTransaction
import com.decentralchain.transaction.smart.script.ScriptCompiler
import com.decentralchain.transaction.transfer.TransferTransaction
import org.scalatest.CancelAfterFailure

class BigLetChain extends BaseTransactionSuite with CancelAfterFailure {
  test("big let assignment chain") {
    val count = 280
    val scriptText =
      s"""
         | {-# STDLIB_VERSION 3    #-}
         | {-# CONTENT_TYPE   DAPP #-}
         |
         | @Verifier(tx)
         | func verify() = {
         |   let a0 = 1
         |   ${1 to count map (i => s"let a$i = a${i - 1}") mkString "\n"}
         |   a$count == a$count
         | }
       """.stripMargin

    val compiledScript = ScriptCompiler.compile(scriptText, ScriptEstimatorV2).explicitGet()._1

    val pkNewAddress = sender.createKeyPair()

    sender.transfer(firstKeyPair, pkNewAddress.toAddress.toString, 10.unitoken, minFee, waitForTx = true)

    val scriptSet          = SetScriptTransaction.selfSigned(1.toByte, pkNewAddress, Some(compiledScript), setScriptFee, System.currentTimeMillis())
    val scriptSetBroadcast = sender.signedBroadcast(scriptSet.explicitGet().json())
    nodes.waitForHeightAriseAndTxPresent(scriptSetBroadcast.id)

    val transfer = TransferTransaction.selfSigned(
      2.toByte,
      pkNewAddress,
      pkNewAddress.toAddress,
      unitoken,
      1.unitoken,
      unitoken,
      smartMinFee,
      ByteStr.empty,
      System.currentTimeMillis()
    )
    val transferBroadcast = sender.signedBroadcast(transfer.explicitGet().json())
    nodes.waitForHeightAriseAndTxPresent(transferBroadcast.id)
  }
}
