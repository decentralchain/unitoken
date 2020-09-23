package com.decentralchain.it.sync.smartcontract

import com.decentralchain.api.http.ApiError.ScriptExecutionError
import com.decentralchain.common.utils.EitherExt2
import com.decentralchain.it.api.SyncHttpApi._
import com.decentralchain.it.sync._
import com.decentralchain.it.transactions.BaseTransactionSuite
import com.decentralchain.it.util._
import com.decentralchain.lang.v1.estimator.v2.ScriptEstimatorV2
import com.decentralchain.transaction.Asset
import com.decentralchain.transaction.smart.InvokeScriptTransaction
import com.decentralchain.transaction.smart.script.ScriptCompiler
import org.scalatest.CancelAfterFailure

class InvokeScriptErrorMsgSuite extends BaseTransactionSuite with CancelAfterFailure {
  private def contract = firstKeyPair
  private def caller   = secondKeyPair

  private lazy val contractAddress: String = contract.toAddress.toString

  protected override def beforeAll(): Unit = {
    super.beforeAll()

    sender.transfer(sender.keyPair, recipient = contractAddress, assetId = None, amount = 5.unitoken, fee = minFee, waitForTx = true).id
    sender.transfer(sender.keyPair, recipient = contractAddress, assetId = None, amount = 5.unitoken, fee = minFee, waitForTx = true).id

    val scriptText =
      """
        |{-# STDLIB_VERSION 3 #-}
        |{-# CONTENT_TYPE DAPP #-}
        |
        |@Callable(inv)
        |func f() = {
        | let pmt = inv.payment.extract()
        | TransferSet([ScriptTransfer(inv.caller, 1, pmt.assetId),
        | ScriptTransfer(inv.caller, 1, pmt.assetId),
        | ScriptTransfer(inv.caller, 1, pmt.assetId),
        | ScriptTransfer(inv.caller, 1, pmt.assetId),
        | ScriptTransfer(inv.caller, 1, pmt.assetId),
        | ScriptTransfer(inv.caller, 1, pmt.assetId),
        | ScriptTransfer(inv.caller, 1, pmt.assetId),
        | ScriptTransfer(inv.caller, 1, pmt.assetId),
        | ScriptTransfer(inv.caller, 1, pmt.assetId),
        | ScriptTransfer(inv.caller, 1, pmt.assetId)])
        |}
        |""".stripMargin
    val script = ScriptCompiler.compile(scriptText, ScriptEstimatorV2).explicitGet()._1.bytes().base64
    sender.setScript(contract, Some(script), setScriptFee, waitForTx = true).id

    sender.setScript(caller, Some(scriptBase64), setScriptFee, waitForTx = true).id
  }

  test("error message is informative") {
    val asset1 = sender
      .issue(
        caller,
        "MyAsset1",
        "Test Asset #1",
        someAssetAmount,
        0,
        fee = issueFee + 400000,
        script = Some(scriptBase64),
        waitForTx = true
      )
      .id

    assertBadRequestAndMessage(
      sender.invokeScript(
        caller,
        contractAddress,
        Some("f"),
        payment = Seq(
          InvokeScriptTransaction.Payment(10, Asset.fromString(Some(asset1)))
        ),
        fee = 1000
      ),
      "State check failed. Reason: Transaction sent from smart account. Requires 400000 extra fee. Transaction involves 1 scripted assets." +
        " Requires 400000 extra fee. Fee for InvokeScriptTransaction (1000 in unitoken) does not exceed minimal value of 1300000 unitoken."
    )

    assertApiError(
      sender
        .invokeScript(
          caller,
          contractAddress,
          Some("f"),
          payment = Seq(
            InvokeScriptTransaction.Payment(10, Asset.fromString(Some(asset1)))
          ),
          fee = 1300000
        ),
      AssertiveApiError(
        ScriptExecutionError.Id,
        "Error while executing account-script: Fee in unitoken for InvokeScriptTransaction (1300000 in unitoken) with 12 total scripts invoked does not exceed minimal value of 5300000 unitoken."
      )
    )
  }
}
