package com.decentralchain.state.diffs.smart.scenarios

import com.decentralchain.common.utils._
import com.decentralchain.db.WithState
import com.decentralchain.lagonaki.mocks.TestBlock
import com.decentralchain.lang.directives.values.{Expression, V3}
import com.decentralchain.lang.script.v1.ExprScript
import com.decentralchain.lang.utils.compilerContext
import com.decentralchain.lang.v1.compiler.ExpressionCompiler
import com.decentralchain.lang.v1.parser.Parser
import com.decentralchain.state.BinaryDataEntry
import com.decentralchain.state.diffs.ENOUGH_AMT
import com.decentralchain.state.diffs.smart.smartEnabledFS
import com.decentralchain.transaction.smart.SetScriptTransaction
import com.decentralchain.transaction.transfer.TransferTransaction
import com.decentralchain.transaction.{DataTransaction, GenesisTransaction}
import com.decentralchain.{NoShrink, TransactionGen}
import org.scalacheck.Gen
import org.scalatest.PropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class TransferByIdTest extends PropSpec with ScalaCheckPropertyChecks with WithState with TransactionGen with NoShrink {

  val scriptSrc =
    s"""
       |match tx {
       |  case dtx: DataTransaction =>
       |    let txId    = extract(getBinary(dtx.data, "transfer_id"))
       |    let maybeTx = transferTransactionById(txId)
       |
       |    isDefined(maybeTx)
       |
       |  case _ => false
       |}
     """.stripMargin

  val expr = {
    val parsed = Parser.parseExpr(scriptSrc).get.value
    ExpressionCompiler(compilerContext(V3, Expression, isAssetScript = false), parsed).explicitGet()._1
  }

  def preconditions: Gen[(GenesisTransaction, TransferTransaction, SetScriptTransaction, DataTransaction)] =
    for {
      master    <- accountGen
      recipient <- accountGen
      ts        <- positiveIntGen
      genesis = GenesisTransaction.create(master.toAddress, ENOUGH_AMT, ts).explicitGet()
      setScript <- selfSignedSetScriptTransactionGenP(master, ExprScript(V3, expr).explicitGet())
      transfer <- Gen.oneOf[TransferTransaction](
        transferGeneratorP(ts, master, recipient.toAddress, ENOUGH_AMT / 2),
        transferGeneratorPV2(ts, master, recipient.toAddress, ENOUGH_AMT / 2)
      )
      data <- dataTransactionGenP(master, List(BinaryDataEntry("transfer_id", transfer.id())))
    } yield (genesis, transfer, setScript, data)

  property("Transfer by id works fine") {
    forAll(preconditions) {
      case (genesis, transfer, setScript, data) =>
        assertDiffEi(
          Seq(TestBlock.create(Seq(genesis, transfer))),
          TestBlock.create(Seq(setScript, data)),
          smartEnabledFS
        )(_ shouldBe an[Right[_, _]])
    }
  }
}
