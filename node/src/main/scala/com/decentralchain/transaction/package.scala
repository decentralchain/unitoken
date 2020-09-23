package com.decentralchain

import cats.data.ValidatedNel
import com.decentralchain.account.PrivateKey
import com.decentralchain.block.{Block, MicroBlock}
import com.decentralchain.common.state.ByteStr
import com.decentralchain.lang.ValidationError
import com.decentralchain.state.Diff
import com.decentralchain.transaction.validation.TxValidator
import com.decentralchain.utils.base58Length

package object transaction {
  val AssetIdLength: Int       = com.decentralchain.crypto.DigestLength
  val AssetIdStringLength: Int = base58Length(AssetIdLength)

  type DiscardedBlocks       = Seq[(Block, ByteStr)]
  type DiscardedMicroBlocks  = Seq[(MicroBlock, Diff)]
  type AuthorizedTransaction = Authorized with Transaction

  type TxType = Byte

  type TxVersion = Byte
  object TxVersion {
    val V1: TxVersion = 1.toByte
    val V2: TxVersion = 2.toByte
    val V3: TxVersion = 3.toByte
  }
  type TxAmount    = Long
  type TxTimestamp = Long
  type TxByteArray = Array[Byte]

  implicit class TransactionValidationOps[T <: Transaction: TxValidator](tx: T) {
    def validatedNel: ValidatedNel[ValidationError, T] = implicitly[TxValidator[T]].validate(tx)
    def validatedEither: Either[ValidationError, T]    = this.validatedNel.toEither.left.map(_.head)
  }

  implicit class TransactionSignOps[T](tx: T)(implicit sign: (T, PrivateKey) => T) {
    def signWith(privateKey: PrivateKey): T = sign(tx, privateKey)
  }
}
