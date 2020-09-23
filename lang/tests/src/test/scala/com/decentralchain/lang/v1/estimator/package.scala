package com.decentralchain.lang.v1

import cats.implicits._
import com.decentralchain.lang.Common
import com.decentralchain.lang.directives.DirectiveSet
import com.decentralchain.lang.directives.values.V3
import com.decentralchain.lang.v1.compiler.Terms
import com.decentralchain.lang.v1.evaluator.EvaluatorV2
import com.decentralchain.lang.v1.evaluator.ctx.LoggedEvaluationContext
import com.decentralchain.lang.v1.evaluator.ctx.impl.PureContext
import com.decentralchain.lang.v1.evaluator.ctx.impl.unitoken.unitokenContext
import com.decentralchain.lang.v1.traits.Environment
import monix.eval.Coeval

package object estimator {
  private val version = V3
  private val ctx =
    PureContext.build(version).withEnvironment[Environment] |+|
    unitokenContext.build(DirectiveSet.contractDirectiveSet)

  private val environment = Common.emptyBlockchainEnvironment()
  private val evaluator =
    new EvaluatorV2(LoggedEvaluationContext(_ => _ => (), ctx.evaluationContext(environment)), version)

  val evaluatorV2AsEstimator = new ScriptEstimator {
    override val version: Int = 0

    override def apply(declaredVals: Set[String], functionCosts: Map[FunctionHeader, Coeval[Long]], expr: Terms.EXPR): Either[String, Long] =
      Right(evaluator(expr, 4000)._2)
  }
}
