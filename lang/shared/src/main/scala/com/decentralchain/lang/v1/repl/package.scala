package com.decentralchain.lang.v1

import cats.implicits._
import com.decentralchain.common.utils.EitherExt2
import com.decentralchain.lang.directives.DirectiveSet
import com.decentralchain.lang.directives.values._
import com.decentralchain.lang.v1.evaluator.ctx.impl.unitoken.unitokenContext
import com.decentralchain.lang.v1.evaluator.ctx.impl.{CryptoContext, PureContext}
import com.decentralchain.lang.v1.repl.node.ErrorMessageEnvironment
import com.decentralchain.lang.v1.repl.node.http.{NodeConnectionSettings, WebEnvironment}
import com.decentralchain.lang.v1.traits.Environment

import scala.concurrent.Future

package object repl {
  val global: BaseGlobal = com.decentralchain.lang.Global
  val internalVarPrefixes: Set[Char] = Set('@', '$')
  val internalFuncPrefix: String = "_"

  val version = V4
  val directives: DirectiveSet = DirectiveSet(version, Account, DApp).explicitGet()

  val initialCtx: CTX[Environment] =
    CryptoContext.build(global, version).withEnvironment[Environment]  |+|
    PureContext.build(version).withEnvironment[Environment] |+|
    unitokenContext.build(directives)

  def buildEnvironment(settings: Option[NodeConnectionSettings]): Environment[Future] =
    settings.fold(ErrorMessageEnvironment: Environment[Future])(WebEnvironment)
}
