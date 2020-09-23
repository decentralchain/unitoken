package com.decentralchain.state.diffs.smart.scenarios

import com.decentralchain.common.utils.EitherExt2
import com.decentralchain.db.WithState
import com.decentralchain.lagonaki.mocks.TestBlock
import com.decentralchain.lang.directives.values._
import com.decentralchain.lang.utils._
import com.decentralchain.lang.v1.compiler.ExpressionCompiler
import com.decentralchain.lang.v1.parser.Parser
import com.decentralchain.state.diffs.produce
import com.decentralchain.state.diffs.smart._
import com.decentralchain.transaction.GenesisTransaction
import com.decentralchain.transaction.lease.LeaseTransaction
import com.decentralchain.transaction.smart.SetScriptTransaction
import com.decentralchain.transaction.transfer._
import com.decentralchain.{NoShrink, TransactionGen}
import org.scalacheck.Gen
import org.scalatest.PropSpec
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}

class TransactionFieldAccessTest extends PropSpec with PropertyChecks with WithState with TransactionGen with NoShrink {

  private def preconditionsTransferAndLease(
      code: String): Gen[(GenesisTransaction, SetScriptTransaction, LeaseTransaction, TransferTransaction)] = {
    val untyped = Parser.parseExpr(code).get.value
    val typed   = ExpressionCompiler(compilerContext(V1, Expression, isAssetScript = false), untyped).explicitGet()._1
    preconditionsTransferAndLease(typed)
  }

  private val script =
    """
      |
      | match tx {
      | case ttx: TransferTransaction =>
      |       isDefined(ttx.assetId)==false
      | case _ =>
      |       false
      | }
      """.stripMargin

  property("accessing field of transaction without checking its type first results on exception") {
    forAll(preconditionsTransferAndLease(script)) {
      case ((genesis, script, lease, transfer)) =>
        assertDiffAndState(Seq(TestBlock.create(Seq(genesis, script))), TestBlock.create(Seq(transfer)), smartEnabledFS) { case _ => () }
        assertDiffEi(Seq(TestBlock.create(Seq(genesis, script))), TestBlock.create(Seq(lease)), smartEnabledFS)(totalDiffEi =>
          totalDiffEi should produce("TransactionNotAllowedByScript"))
    }
  }
}
