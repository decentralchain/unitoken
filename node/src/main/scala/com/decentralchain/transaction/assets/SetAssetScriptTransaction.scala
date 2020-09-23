package com.decentralchain.transaction.assets

import com.decentralchain.account._
import com.decentralchain.crypto
import com.decentralchain.lang.ValidationError
import com.decentralchain.lang.script.Script
import com.decentralchain.transaction.Asset.IssuedAsset
import com.decentralchain.transaction._
import com.decentralchain.transaction.serialization.impl.SetAssetScriptTxSerializer
import com.decentralchain.transaction.validation.TxValidator
import com.decentralchain.transaction.validation.impl.SetAssetScriptTxValidator
import monix.eval.Coeval
import play.api.libs.json.JsObject

import scala.util.Try

case class SetAssetScriptTransaction(
    version: TxVersion,
    sender: PublicKey,
    asset: IssuedAsset,
    script: Option[Script],
    fee: TxAmount,
    timestamp: TxTimestamp,
    proofs: Proofs,
    chainId: Byte
) extends VersionedTransaction
    with ProvenTransaction
    with TxWithFee.Inunitoken
    with FastHashId
    with LegacyPBSwitch.V2 {

  //noinspection TypeAnnotation
  override val builder = SetAssetScriptTransaction

  override val bodyBytes: Coeval[Array[Byte]] = Coeval.evalOnce(builder.serializer.bodyBytes(this))
  override val bytes: Coeval[Array[Byte]]     = Coeval.evalOnce(builder.serializer.toBytes(this))
  override val json: Coeval[JsObject]         = Coeval.evalOnce(builder.serializer.toJson(this))

  override val checkedAssets: Seq[IssuedAsset] = Seq(asset)
}

object SetAssetScriptTransaction extends TransactionParser {
  type TransactionT = SetAssetScriptTransaction

  override val typeId: TxType                    = 15: Byte
  override val supportedVersions: Set[TxVersion] = Set(1, 2)

  implicit val validator: TxValidator[SetAssetScriptTransaction] = SetAssetScriptTxValidator

  implicit def sign(tx: SetAssetScriptTransaction, privateKey: PrivateKey): SetAssetScriptTransaction =
    tx.copy(proofs = Proofs(crypto.sign(privateKey, tx.bodyBytes())))

  val serializer = SetAssetScriptTxSerializer

  override def parseBytes(bytes: Array[TxVersion]): Try[SetAssetScriptTransaction] =
    serializer.parseBytes(bytes)

  def create(
      version: TxVersion,
      sender: PublicKey,
      assetId: IssuedAsset,
      script: Option[Script],
      fee: TxAmount,
      timestamp: TxTimestamp,
      proofs: Proofs,
      chainId: Byte = AddressScheme.current.chainId
  ): Either[ValidationError, SetAssetScriptTransaction] =
    SetAssetScriptTransaction(version, sender, assetId, script, fee, timestamp, proofs, chainId).validatedEither

  def signed(
      version: TxVersion,
      sender: PublicKey,
      asset: IssuedAsset,
      script: Option[Script],
      fee: TxAmount,
      timestamp: TxTimestamp,
      signer: PrivateKey
  ): Either[ValidationError, SetAssetScriptTransaction] =
    create(version, sender, asset, script, fee, timestamp, Proofs.empty).map(_.signWith(signer))

  def selfSigned(
      version: TxVersion,
      sender: KeyPair,
      asset: IssuedAsset,
      script: Option[Script],
      fee: TxAmount,
      timestamp: TxTimestamp
  ): Either[ValidationError, SetAssetScriptTransaction] =
    signed(version, sender.publicKey, asset, script, fee, timestamp, sender.privateKey)
}
