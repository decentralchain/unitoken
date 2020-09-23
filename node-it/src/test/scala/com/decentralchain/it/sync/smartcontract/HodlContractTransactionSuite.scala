package com.decentralchain.it.sync.smartcontract

import com.decentralchain.common.utils.EitherExt2
import com.decentralchain.it.api.SyncHttpApi._
import com.decentralchain.it.api.{PutDataResponse, TransactionInfo}
import com.decentralchain.it.sync.{minFee, setScriptFee}
import com.decentralchain.it.transactions.BaseTransactionSuite
import com.decentralchain.it.util._
import com.decentralchain.lang.v1.compiler.Terms.CONST_LONG
import com.decentralchain.lang.v1.estimator.v2.ScriptEstimatorV2
import com.decentralchain.state._
import com.decentralchain.transaction.Asset.unitoken
import com.decentralchain.transaction.smart.InvokeScriptTransaction
import com.decentralchain.transaction.smart.script.ScriptCompiler
import org.scalatest.CancelAfterFailure

class HodlContractTransactionSuite extends BaseTransactionSuite with CancelAfterFailure {

  private def contract = firstKeyPair
  private def caller   = secondKeyPair

  private lazy val contractAddress: String = contract.toAddress.toString
  private lazy val callerAddress: String   = caller.toAddress.toString

  test("setup contract account with unitoken") {
    sender
      .transfer(
        sender.keyPair,
        recipient = contractAddress,
        assetId = None,
        amount = 5.unitoken,
        fee = minFee,
        waitForTx = true
      )
      .id
  }

  test("setup caller account with unitoken") {
    sender
      .transfer(
        sender.keyPair,
        recipient = callerAddress,
        assetId = None,
        amount = 10.unitoken,
        fee = minFee,
        waitForTx = true
      )
      .id
  }

  test("set contract to contract account") {
    val scriptText =
      """
        |{-# STDLIB_VERSION 3 #-}
        |{-# CONTENT_TYPE DAPP #-}
        |
        |	@Callable(i)
        |	func deposit() = {
        |   let pmt = extract(i.payment)
        |   if (isDefined(pmt.assetId)) then throw("can hodl unitoken only at the moment")
        |   else {
        |	  	let currentKey = toBase58String(i.caller.bytes)
        |	  	let currentAmount = match getInteger(this, currentKey) {
        |	  		case a:Int => a
        |	  		case _ => 0
        |	  	}
        |	  	let newAmount = currentAmount + pmt.amount
        |	  	WriteSet([DataEntry(currentKey, newAmount)])
        |
        |   }
        |	}
        |
        | @Callable(i)
        | func withdraw(amount: Int) = {
        |	  	let currentKey = toBase58String(i.caller.bytes)
        |	  	let currentAmount = match getInteger(this, currentKey) {
        |	  		case a:Int => a
        |	  		case _ => 0
        |	  	}
        |		let newAmount = currentAmount - amount
        |	 if (amount < 0)
        |			then throw("Can't withdraw negative amount")
        |  else if (newAmount < 0)
        |			then throw("Not enough balance")
        |			else  ScriptResult(
        |					WriteSet([DataEntry(currentKey, newAmount)]),
        |					TransferSet([ScriptTransfer(i.caller, amount, unit)])
        |				)
        |	}
        """.stripMargin

    val script      = ScriptCompiler.compile(scriptText, ScriptEstimatorV2).explicitGet()._1.bytes().base64
    val setScriptId = sender.setScript(contract, Some(script), setScriptFee, waitForTx = true).id

    val acc0ScriptInfo = sender.addressScriptInfo(contractAddress)

    acc0ScriptInfo.script.isEmpty shouldBe false
    acc0ScriptInfo.scriptText.isEmpty shouldBe false
    acc0ScriptInfo.script.get.startsWith("base64:") shouldBe true

    sender.transactionInfo[TransactionInfo](setScriptId).script.get.startsWith("base64:") shouldBe true
  }

  test("caller deposits unitoken") {
    val balanceBefore = sender.accountBalances(contractAddress)._1
    val invokeScriptId = sender
      .invokeScript(
        caller,
        dappAddress = contractAddress,
        func = Some("deposit"),
        args = List.empty,
        payment = Seq(InvokeScriptTransaction.Payment(1.5.unitoken, unitoken)),
        fee = 1.unitoken,
        waitForTx = true
      )
      ._1
      .id

    sender.waitForTransaction(invokeScriptId)

    sender.getDataByKey(contractAddress, callerAddress) shouldBe IntegerDataEntry(callerAddress, 1.5.unitoken)
    val balanceAfter = sender.accountBalances(contractAddress)._1

    (balanceAfter - balanceBefore) shouldBe 1.5.unitoken
  }

  test("caller can't withdraw more than owns") {
    assertBadRequestAndMessage(
      sender
        .invokeScript(
          caller,
          contractAddress,
          func = Some("withdraw"),
          args = List(CONST_LONG(1.51.unitoken)),
          payment = Seq(),
          fee = 1.unitoken
        ),
      "Not enough balance"
    )
  }

  test("caller can withdraw less than he owns") {
    val balanceBefore = sender.accountBalances(contractAddress)._1
    val invokeScriptId = sender
      .invokeScript(
        caller,
        dappAddress = contractAddress,
        func = Some("withdraw"),
        args = List(CONST_LONG(1.49.unitoken)),
        payment = Seq(),
        fee = 1.unitoken,
        waitForTx = true
      )
      ._1
      .id

    val balanceAfter = sender.accountBalances(contractAddress)._1

    sender.getDataByKey(contractAddress, callerAddress) shouldBe IntegerDataEntry(callerAddress, 0.01.unitoken)
    (balanceAfter - balanceBefore) shouldBe -1.49.unitoken

    val stateChangesInfo = sender.debugStateChanges(invokeScriptId).stateChanges

    val stateChangesData = stateChangesInfo.get.data.head.asInstanceOf[PutDataResponse]
    stateChangesInfo.get.data.length shouldBe 1
    stateChangesData.`type` shouldBe "integer"
    stateChangesData.value.asInstanceOf[Long] shouldBe 0.01.unitoken

    val stateChangesTransfers = stateChangesInfo.get.transfers.head
    stateChangesInfo.get.transfers.length shouldBe 1
    stateChangesTransfers.address shouldBe callerAddress
    stateChangesTransfers.amount shouldBe 1.49.unitoken
    stateChangesTransfers.asset shouldBe None
  }

}
