package com.decentralchain.transaction.lease

import com.decentralchain.account.{AddressScheme, PrivateKey, PublicKey}
import com.decentralchain.common.state.ByteStr
import com.decentralchain.crypto
import com.decentralchain.lang.ValidationError
import com.decentralchain.transaction._
import com.decentralchain.transaction.serialization.impl.LeaseCancelTxSerializer
import com.decentralchain.transaction.validation.TxValidator
import com.decentralchain.transaction.validation.impl.LeaseCancelTxValidator
import monix.eval.Coeval
import play.api.libs.json.JsObject

import scala.util.Try

final case class LeaseCancelTransaction(
    version: TxVersion,
    sender: PublicKey,
    leaseId: ByteStr,
    fee: TxAmount,
    timestamp: TxTimestamp,
    proofs: Proofs,
    chainId: Byte
) extends SigProofsSwitch
    with VersionedTransaction
    with TxWithFee.Inunitoken
    with FastHashId
    with LegacyPBSwitch.V3 {
  override def builder: TransactionParser          = LeaseCancelTransaction
  override val bodyBytes: Coeval[Array[TxVersion]] = Coeval.evalOnce(LeaseCancelTxSerializer.bodyBytes(this))
  override val bytes: Coeval[Array[TxVersion]]     = Coeval.evalOnce(LeaseCancelTxSerializer.toBytes(this))
  override val json: Coeval[JsObject]              = Coeval.evalOnce(LeaseCancelTxSerializer.toJson(this))
}

object LeaseCancelTransaction extends TransactionParser {
  type TransactionT = LeaseCancelTransaction

  val supportedVersions: Set[TxVersion] = Set(1, 2, 3)
  val typeId: TxType                    = 9: Byte

  implicit val validator: TxValidator[LeaseCancelTransaction] = LeaseCancelTxValidator

  implicit def sign(tx: LeaseCancelTransaction, privateKey: PrivateKey): LeaseCancelTransaction =
    tx.copy(proofs = Proofs(crypto.sign(privateKey, tx.bodyBytes())))

  override def parseBytes(bytes: Array[Byte]): Try[LeaseCancelTransaction] =
    LeaseCancelTxSerializer.parseBytes(bytes)

  def create(
      version: TxVersion,
      sender: PublicKey,
      leaseId: ByteStr,
      fee: TxAmount,
      timestamp: TxTimestamp,
      proofs: Proofs,
      chainId: Byte = AddressScheme.current.chainId
  ): Either[ValidationError, TransactionT] =
    LeaseCancelTransaction(version, sender, leaseId, fee, timestamp, proofs, chainId).validatedEither

  def signed(
      version: TxVersion,
      sender: PublicKey,
      leaseId: ByteStr,
      fee: TxAmount,
      timestamp: TxTimestamp,
      signer: PrivateKey
  ): Either[ValidationError, TransactionT] =
    create(version, sender, leaseId, fee, timestamp, Nil).map(_.signWith(signer))
}
