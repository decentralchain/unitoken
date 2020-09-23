package com.decentralchain.transaction.smart

import cats.Id
import cats.implicits._
import com.decentralchain.common.state.ByteStr
import com.decentralchain.lang.directives.DirectiveSet
import com.decentralchain.lang.directives.values.{ContentType, ScriptType, StdLibVersion}
import com.decentralchain.lang.v1.evaluator.ctx.EvaluationContext
import com.decentralchain.lang.v1.evaluator.ctx.impl.unitoken.unitokenContext
import com.decentralchain.lang.v1.evaluator.ctx.impl.{CryptoContext, PureContext}
import com.decentralchain.lang.v1.traits.Environment
import com.decentralchain.lang.{ExecutionError, Global}
import com.decentralchain.state._
import monix.eval.Coeval

object BlockchainContext {

  type In = unitokenEnvironment.In
  def build(version: StdLibVersion,
            nByte: Byte,
            in: Coeval[Environment.InputEntity],
            h: Coeval[Int],
            blockchain: Blockchain,
            isTokenContext: Boolean,
            isContract: Boolean,
            address: Environment.Tthis,
            txId: ByteStr): Either[ExecutionError, EvaluationContext[Environment, Id]] = {
    DirectiveSet(
      version,
      ScriptType.isAssetScript(isTokenContext),
      ContentType.isDApp(isContract)
    ).map { ds =>
      val ctx =
        PureContext.build(version).withEnvironment[Environment]   |+|
        CryptoContext.build(Global, version).withEnvironment[Environment] |+|
        unitokenContext.build(ds)

      ctx.evaluationContext(new unitokenEnvironment(nByte, in, h, blockchain, address, ds, txId))
    }
  }
}
