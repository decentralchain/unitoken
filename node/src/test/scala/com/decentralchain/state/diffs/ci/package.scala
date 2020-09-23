package com.decentralchain.state.diffs

import cats.implicits._
import com.decentralchain.common.utils.EitherExt2
import com.decentralchain.lang.Global
import com.decentralchain.lang.contract.DApp
import com.decentralchain.lang.directives.DirectiveSet
import com.decentralchain.lang.directives.values.{Account, Expression, ScriptType, StdLibVersion, V3, DApp => DAppType}
import com.decentralchain.lang.v1.compiler.{ContractCompiler, ExpressionCompiler, Terms}
import com.decentralchain.lang.v1.evaluator.ctx.impl.unitoken.unitokenContext
import com.decentralchain.lang.v1.evaluator.ctx.impl.{CryptoContext, PureContext}
import com.decentralchain.lang.v1.parser.Expressions.{DAPP, EXPR}
import com.decentralchain.lang.v1.traits.Environment
import com.decentralchain.state.diffs.FeeValidation._
import com.decentralchain.transaction.assets.IssueTransaction
import com.decentralchain.transaction.smart.InvokeScriptTransaction
import org.scalacheck.Gen

package object ci {
  def ciFee(sc: Int = 0, nonNftIssue: Int = 0): Gen[Long] =
    Gen.choose(
      FeeUnit * FeeConstants(InvokeScriptTransaction.typeId) + sc * ScriptExtraFee + nonNftIssue * FeeConstants(IssueTransaction.typeId) * FeeUnit,
      FeeUnit * FeeConstants(InvokeScriptTransaction.typeId) + (sc + 1) * ScriptExtraFee - 1 + nonNftIssue * FeeConstants(IssueTransaction.typeId) * FeeUnit
    )

  def compileContractFromExpr(expr: DAPP, version: StdLibVersion = V3): DApp = {
    val ctx =
      PureContext.build(version).withEnvironment[Environment] |+|
        CryptoContext.build(Global, version).withEnvironment[Environment] |+|
        unitokenContext.build(
          DirectiveSet(version, Account, DAppType).explicitGet()
        )
    ContractCompiler(ctx.compilerContext, expr, version).explicitGet()
  }

  def compileExpr(expr: EXPR, version: StdLibVersion, scriptType: ScriptType): Terms.EXPR = {
    val ctx =
      PureContext.build(version).withEnvironment[Environment] |+|
        CryptoContext.build(Global, version).withEnvironment[Environment] |+|
        unitokenContext.build(
          DirectiveSet(version, scriptType, Expression).explicitGet()
        )
    ExpressionCompiler(ctx.compilerContext, expr).explicitGet()._1
  }
}
