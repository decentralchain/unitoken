package com.decentralchain.lang

import com.decentralchain.lang.directives.values._
import com.decentralchain.lang.v1.evaluator.ctx.impl.unitoken.Types
import org.scalatest.{FreeSpec, Matchers}

class ContextVersionTest extends FreeSpec with Matchers {

  "InvokeScriptTransaction" - {
    "exist in lib version 3" in {
      val types = Types.buildunitokenTypes(true, V3)
      types.count(c => c.name == "InvokeScriptTransaction") shouldEqual 1
    }

    "doesn't exist in lib version 2" in {
      val types = Types.buildunitokenTypes(true, V2)
      types.count(c => c.name == "InvokeScriptTransaction") shouldEqual 0
    }
  }
}
