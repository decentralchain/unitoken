package com.decentralchain.transaction

import cats.implicits._
import com.decentralchain.common.state.ByteStr
import com.decentralchain.lang.ExecutionError
import com.decentralchain.lang.directives.DirectiveSet
import com.decentralchain.lang.directives.values.{Account, Expression, Asset => AssetType, DApp => DAppType}
import com.decentralchain.lang.v1.traits.Environment.{InputEntity, Tthis}
import com.decentralchain.state.Blockchain
import com.decentralchain.transaction.smart.script.ScriptRunner.TxOrd
import com.decentralchain.transaction.smart.{DApp => DAppTarget}
import shapeless._

package object smart {
  def buildThisValue(
      in: TxOrd,
      blockchain: Blockchain,
      ds: DirectiveSet,
      scriptContainerAddress: Tthis
  ): Either[ExecutionError, InputEntity] =
    in.eliminate(
      tx =>
        RealTransactionWrapper(tx, blockchain, ds.stdLibVersion, paymentTarget(ds, scriptContainerAddress))
          .map(Coproduct[InputEntity](_)),
      _.eliminate(
        order => Coproduct[InputEntity](RealTransactionWrapper.ord(order)).asRight[ExecutionError],
        _.eliminate(
          scriptTransfer => Coproduct[InputEntity](scriptTransfer).asRight[ExecutionError],
          _ => ???
        )
      )
    )

  def paymentTarget(
      ds: DirectiveSet,
      scriptContainerAddress: Tthis
  ): AttachedPaymentTarget =
    (ds.scriptType, ds.contentType) match {
      case (Account, DAppType)                 => DAppTarget
      case (Account, Expression)               => InvokerScript
      case (AssetType, Expression) => scriptContainerAddress.eliminate(_ => throw new Exception("Not a AssetId"), _.eliminate(a => AssetScript(ByteStr(a.id)), v => throw new Exception(s"Fail processing tthis value $v")))
      case _                                      => ???
    }
}
