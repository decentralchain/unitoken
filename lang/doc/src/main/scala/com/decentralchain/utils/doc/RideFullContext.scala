package com.decentralchain.utils.doc

import cats.implicits._
import com.decentralchain.lang.Global
import com.decentralchain.lang.directives.DirectiveSet
import com.decentralchain.lang.v1.CTX
import com.decentralchain.lang.v1.evaluator.ctx.impl.unitoken.unitokenContext
import com.decentralchain.lang.v1.evaluator.ctx.impl.{CryptoContext, PureContext}
import com.decentralchain.lang.v1.traits.Environment

object RideFullContext {
  def build(ds: DirectiveSet): CTX[Environment] = {
    val unitokenCtx  = unitokenContext.build(ds)
    val cryptoCtx = CryptoContext.build(Global, ds.stdLibVersion).withEnvironment[Environment]
    val pureCtx = PureContext.build(ds).withEnvironment[Environment]
    pureCtx |+| cryptoCtx |+| unitokenCtx
  }
}
