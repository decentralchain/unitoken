package com.decentralchain.it.sync.smartcontract

import com.decentralchain.api.http.ApiError._
import com.decentralchain.common.state.ByteStr
import com.decentralchain.common.utils.EitherExt2
import com.decentralchain.it.api.SyncHttpApi._
import com.decentralchain.it.sync._
import com.decentralchain.it.transactions.BaseTransactionSuite
import com.decentralchain.it.util._
import com.decentralchain.lang.v1.compiler.Terms.CONST_STRING
import com.decentralchain.lang.v1.estimator.v2.ScriptEstimatorV2
import com.decentralchain.state._
import com.decentralchain.transaction.Asset.{IssuedAsset, unitoken}
import com.decentralchain.transaction.smart.InvokeScriptTransaction.Payment
import com.decentralchain.transaction.smart.script.ScriptCompiler
import org.scalatest.CancelAfterFailure

class InvokeMultiplePaymentsSuite extends BaseTransactionSuite with CancelAfterFailure {
  private def dApp   = firstKeyPair
  private def caller = secondKeyPair

  private lazy val dAppAddress: String   = dApp.toAddress.toString
  private lazy val callerAddress: String = caller.toAddress.toString

  private var asset1: IssuedAsset = _
  private var asset2: IssuedAsset = _

  test("prerequisite: set contract and issue asset") {
    val source =
      s"""
      |{-# STDLIB_VERSION 4 #-}
      |{-# CONTENT_TYPE DAPP #-}
      |{-# SCRIPT_TYPE ACCOUNT #-}
      |
      |func parse(asset: ByteVector|Unit) = if asset.isDefined() then asset.value() else base58''
      |
      |@Callable(inv)
      |func default() = {
      |  let pmt = inv.payments
      |  nil
      |  ++ (if pmt.size() > 0 then [
      |    IntegerEntry("amount_0", pmt[0].amount),
      |    BinaryEntry("asset_0", pmt[0].assetId.parse())
      |  ] else nil)
      |  ++ (if pmt.size() > 1 then [
      |    IntegerEntry("amount_1", pmt[1].amount),
      |    BinaryEntry("asset_1", pmt[1].assetId.parse())
      |  ] else nil)
      |}
      |
      |@Callable(inv)
      |func f(toAlias: String) = {
      | if (${"sigVerify(base58'', base58'', base58'') ||" * 8} true)
      |  then {
      |    let pmt = inv.payments[0]
      |    #avoidbugcomment
      |    [ScriptTransfer(Alias(toAlias), pmt.amount, pmt.assetId)]
      |  }
      |  else {
      |    throw("unexpected")
      |  }
      |}
      """.stripMargin
    val script = ScriptCompiler.compile(source, ScriptEstimatorV2).explicitGet()._1.bytes().base64
    sender.setScript(dApp, Some(script), setScriptFee, waitForTx = true)

    asset1 = IssuedAsset(ByteStr.decodeBase58(sender.issue(caller, waitForTx = true).id).get)
    asset2 = IssuedAsset(ByteStr.decodeBase58(sender.issue(caller, waitForTx = true).id).get)
    sender.createAlias(caller, "recipientalias", smartMinFee, waitForTx = true)
  }

  test("can transfer to alias") {
    val dAppBalance   = sender.balance(dAppAddress).balance
    val callerBalance = sender.balance(callerAddress).balance

    sender
      .invokeScript(
        caller,
        dAppAddress,
        Some("f"),
        payment = Seq(Payment(1.unitoken, unitoken)),
        args = List(CONST_STRING("recipientalias").explicitGet()),
        waitForTx = true
      )

    sender.balance(dAppAddress).balance shouldBe dAppBalance
    sender.balance(callerAddress).balance shouldBe callerBalance - smartMinFee
  }

  test("script should sheck if alias not exists") {
    val alias = "unknown"

    assertBadRequestAndMessage(
      sender
        .invokeScript(
          caller,
          dAppAddress,
          Some("f"),
          payment = Seq(Payment(1.unitoken, unitoken)),
          args = List(CONST_STRING(alias).explicitGet())
        ),
      s"Alias 'alias:I:$alias"
    )

    assertBadRequestAndMessage(
      sender
        .invokeScript(
          caller,
          dAppAddress,
          Some("f"),
          payment = Seq(Payment(1.unitoken, unitoken)),
          args = List(CONST_STRING(s"alias:I:$alias").explicitGet()),
          waitForTx = true
        ),
      "Alias should contain only following characters"
    )
  }

  test("can invoke with no payments") {
    sender.invokeScript(caller, dAppAddress, payment = Seq.empty, waitForTx = true)
    sender.getData(dAppAddress).size shouldBe 0
  }

  test("can invoke with single payment of unitoken") {
    sender.invokeScript(caller, dAppAddress, payment = Seq(Payment(1.unitoken, unitoken)), waitForTx = true)
    sender.getData(dAppAddress).size shouldBe 2
    sender.getDataByKey(dAppAddress, "amount_0").as[IntegerDataEntry].value shouldBe 1.unitoken
    sender.getDataByKey(dAppAddress, "asset_0").as[BinaryDataEntry].value shouldBe ByteStr.empty
  }

  test("can invoke with single payment of asset") {
    sender.invokeScript(caller, dAppAddress, payment = Seq(Payment(10, asset1)), waitForTx = true)
    sender.getData(dAppAddress).size shouldBe 2
    sender.getDataByKey(dAppAddress, "amount_0").as[IntegerDataEntry].value shouldBe 10
    sender.getDataByKey(dAppAddress, "asset_0").as[BinaryDataEntry].value shouldBe asset1.id
  }

  test("can invoke with two payments of unitoken") {
    sender.invokeScript(caller, dAppAddress, payment = Seq(Payment(5, unitoken), Payment(17, unitoken)), waitForTx = true)
    sender.getData(dAppAddress).size shouldBe 4
    sender.getDataByKey(dAppAddress, "amount_0").as[IntegerDataEntry].value shouldBe 5
    sender.getDataByKey(dAppAddress, "asset_0").as[BinaryDataEntry].value shouldBe ByteStr.empty
    sender.getDataByKey(dAppAddress, "amount_1").as[IntegerDataEntry].value shouldBe 17
    sender.getDataByKey(dAppAddress, "asset_1").as[BinaryDataEntry].value shouldBe ByteStr.empty
  }

  test("can invoke with two payments of the same asset") {
    sender.invokeScript(caller, dAppAddress, payment = Seq(Payment(8, asset1), Payment(21, asset1)), waitForTx = true)
    sender.getData(dAppAddress).size shouldBe 4
    sender.getDataByKey(dAppAddress, "amount_0").as[IntegerDataEntry].value shouldBe 8
    sender.getDataByKey(dAppAddress, "asset_0").as[BinaryDataEntry].value shouldBe asset1.id
    sender.getDataByKey(dAppAddress, "amount_1").as[IntegerDataEntry].value shouldBe 21
    sender.getDataByKey(dAppAddress, "asset_1").as[BinaryDataEntry].value shouldBe asset1.id
  }

  test("can invoke with two payments of different assets") {
    sender.invokeScript(caller, dAppAddress, payment = Seq(Payment(3, asset1), Payment(6, asset2)), waitForTx = true)
    sender.getData(dAppAddress).size shouldBe 4
    sender.getDataByKey(dAppAddress, "amount_0").as[IntegerDataEntry].value shouldBe 3
    sender.getDataByKey(dAppAddress, "asset_0").as[BinaryDataEntry].value shouldBe asset1.id
    sender.getDataByKey(dAppAddress, "amount_1").as[IntegerDataEntry].value shouldBe 6
    sender.getDataByKey(dAppAddress, "asset_1").as[BinaryDataEntry].value shouldBe asset2.id
  }

  test("can't invoke with three payments") {
    assertApiError(
      sender.invokeScript(
        caller,
        dAppAddress,
        payment = Seq(Payment(3, unitoken), Payment(6, unitoken), Payment(7, unitoken))
      )
    ) { error =>
      error.message should include("Script payment amount=3 should not exceed 2")
      error.id shouldBe StateCheckFailed.Id
      error.statusCode shouldBe 400
    }
  }

  test("can't attach more than balance") {
    val unitokenBalance  = sender.accountBalances(callerAddress)._1
    val asset1Balance = sender.assetBalance(callerAddress, asset1.id.toString).balance

    assertApiError(
      sender.invokeScript(
        caller,
        dAppAddress,
        payment = Seq(Payment(unitokenBalance - 1.unitoken, unitoken), Payment(2.unitoken, unitoken))
      )
    ) { error =>
      error.message should include("Transaction application leads to negative unitoken balance to (at least) temporary negative state")
      error.id shouldBe StateCheckFailed.Id
      error.statusCode shouldBe 400
    }

    assertApiError(
      sender.invokeScript(
        caller,
        dAppAddress,
        payment = Seq(Payment(asset1Balance - 1000, asset1), Payment(1001, asset1))
      )
    ) { error =>
      error.message should include("Transaction application leads to negative asset")
      error.id shouldBe StateCheckFailed.Id
      error.statusCode shouldBe 400
    }
  }

  test("can't attach leased unitoken") {
    val unitokenBalance = sender.accountBalances(callerAddress)._1
    sender.lease(caller, dAppAddress, unitokenBalance - 1.unitoken, waitForTx = true)

    assertApiError(
      sender.invokeScript(caller, dAppAddress, payment = Seq(Payment(0.75.unitoken, unitoken), Payment(0.75.unitoken, unitoken)))
    ) { error =>
      error.message should include("Accounts balance errors")
      error.id shouldBe StateCheckFailed.Id
      error.statusCode shouldBe 400
    }
  }

  test("can't attach with zero unitoken amount") {
    assertApiError(
      sender.invokeScript(caller, dAppAddress, payment = Seq(Payment(1, asset1), Payment(0, unitoken))),
      NonPositiveAmount("0 of unitoken")
    )
  }

  test("can't attach with zero asset amount") {
    assertApiError(
      sender.invokeScript(caller, dAppAddress, payment = Seq(Payment(0, asset1), Payment(1, unitoken))),
      NonPositiveAmount(s"0 of IssuedAsset(${asset1.id.toString})")
    )
  }

}
