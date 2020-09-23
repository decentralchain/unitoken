package com.decentralchain.utils

import cats.Id
import com.decentralchain.lang.directives.DirectiveSet
import com.decentralchain.lang.directives.values.V3
import com.decentralchain.lang.utils._
import com.decentralchain.lang.v1.compiler.Terms.{FUNCTION_CALL, TRUE}
import com.decentralchain.lang.v1.compiler.Types.BOOLEAN
import com.decentralchain.lang.v1.evaluator.ctx.{EvaluationContext, UserFunction}
import com.decentralchain.lang.v1.traits.Environment
import com.decentralchain.state.diffs.smart.predef.chainId
import com.decentralchain.common.state.ByteStr
import com.decentralchain.transaction.smart.unitokenEnvironment
import monix.eval.Coeval
import org.scalatest.{FreeSpec, Matchers}

class UtilsSpecification extends FreeSpec with Matchers {
  private val environment = new unitokenEnvironment(chainId, Coeval(???), null, EmptyBlockchain, null, DirectiveSet.contractDirectiveSet, ByteStr.empty)

  "estimate()" - {
    "handles functions that depend on each other" in {
      val callee = UserFunction[Environment]("callee", 0, BOOLEAN)(TRUE)
      val caller = UserFunction[Environment]("caller", 0, BOOLEAN)(FUNCTION_CALL(callee.header, List.empty))
      val ctx = EvaluationContext.build[Id, Environment](
        environment,
        typeDefs = Map.empty,
        letDefs = Map.empty,
        functions = Seq(caller, callee)
      )
      estimate(V3, ctx).size shouldBe 2
    }
  }
}
