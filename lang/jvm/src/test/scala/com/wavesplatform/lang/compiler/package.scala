package com.wavesplatform.lang

import cats.kernel.Monoid
import com.wavesplatform.lang.Common.multiplierFunction
import com.wavesplatform.lang.v1.CTX
import com.wavesplatform.lang.v1.compiler.Types.TYPEPLACEHOLDER.TYPEPARAM
import com.wavesplatform.lang.v1.compiler.Types._
import com.wavesplatform.lang.v1.evaluator.ctx.impl.PureContext
import com.wavesplatform.lang.v1.evaluator.ctx.{CaseType, NativeFunction}

package object compiler {

  val pointType = CaseType("Point", List("x" -> LONG, "y" -> LONG))

  val idT = NativeFunction("idT", 1, 10000: Short, TYPEPARAM('T'), "p1" -> TYPEPARAM('T'))(Right(_))
  val undefinedOptionLong =
    NativeFunction("undefinedOptionLong", 1, 1002: Short, typeToConcretePlaceholder(UNION(LONG, UNIT)): TYPEPLACEHOLDER)(_ => ???)
  val idOptionLong =
    NativeFunction("idOptionLong", 1, 1003: Short, TYPEPLACEHOLDER.UNIT, ("opt" -> typeToConcretePlaceholder(UNION(LONG, UNIT))))(_ => Right(()))
  val functionWithTwoPrarmsOfTheSameType =
    NativeFunction("functionWithTwoPrarmsOfTheSameType", 1, 1005: Short, TYPEPARAM('T'), ("p1" -> TYPEPARAM('T')), ("p2" -> TYPEPARAM('T')))(l =>
      Right(l.head))

  val compilerContext = Monoid
    .combine(
      PureContext.ctx,
      CTX(
        Seq(pointType, Common.pointTypeA, Common.pointTypeB),
        Map(("p", (Common.AorB, null))),
        Seq(multiplierFunction, functionWithTwoPrarmsOfTheSameType, idT, undefinedOptionLong, idOptionLong)
      )
    )
    .compilerContext

}
