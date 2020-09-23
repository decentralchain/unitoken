package com.decentralchain.lang

import cats.kernel.Monoid
import com.decentralchain.lang.directives.values.V2
import com.decentralchain.lang.v1.compiler.ExpressionCompiler
import com.decentralchain.lang.v1.compiler.Terms.EXPR
import com.decentralchain.lang.v1.evaluator.ctx.impl.unitoken.unitokenContext
import com.decentralchain.lang.v1.evaluator.ctx.impl.{CryptoContext, PureContext}

object JavaAdapter {
  private val version = V2

  lazy val ctx =
    Monoid.combineAll(
      Seq(
        CryptoContext.compilerContext(Global, version),
        unitokenContext.build(???).compilerContext,
        PureContext.build(version).compilerContext
      ))

  def compile(input: String): EXPR = {
    ExpressionCompiler
      .compile(input, ctx)
      .fold(
        error => throw new IllegalArgumentException(error),
        res => res
      )
  }
}
