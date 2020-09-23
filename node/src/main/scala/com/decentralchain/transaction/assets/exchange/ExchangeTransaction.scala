package com.decentralchain.transaction.assets.exchange

import com.decentralchain.account.{AddressScheme, PrivateKey, PublicKey}
import com.decentralchain.crypto
import com.decentralchain.lang.ValidationError
import com.decentralchain.transaction.Asset.IssuedAsset
import com.decentralchain.transaction._
import com.decentralchain.transaction.serialization.impl.ExchangeTxSerializer
import com.decentralchain.transaction.validation.impl.ExchangeTxValidator
import monix.eval.Coeval
import play.api.libs.json.JsObject

import scala.util.Try

case class ExchangeTransaction(
    version: TxVersion,
    order1: Order,
    order2: Order,
    amount: Long,
    price: Long,
    buyMatcherFee: Long,
    sellMatcherFee: Long,
    fee: Long,
    timestamp: Long,
    proofs: Proofs,
    chainId: Byte
) extends VersionedTransaction
    with ProvenTransaction
    with TxWithFee.Inunitoken
    with FastHashId
    with SigProofsSwitch
    with LegacyPBSwitch.V3 {

  val (buyOrder, sellOrder) = if (order1.orderType == OrderType.BUY) (order1, order2) else (order2, order1)

  override def builder: TransactionParser = ExchangeTransaction

  override val sender: PublicKey = buyOrder.matcherPublicKey

  override val bodyBytes: Coeval[Array[Byte]] = Coeval.evalOnce(ExchangeTransaction.serializer.bodyBytes(this))
  override val bytes: Coeval[Array[Byte]]     = Coeval.evalOnce(ExchangeTransaction.serializer.toBytes(this))
  override val json: Coeval[JsObject]         = Coeval.evalOnce(ExchangeTransaction.serializer.toJson(this))

  override def checkedAssets: Seq[IssuedAsset] = {
    val pair = buyOrder.assetPair
    Seq(pair.priceAsset, pair.amountAsset) collect { case a: IssuedAsset => a }
  }
}

object ExchangeTransaction extends TransactionParser {
  type TransactionT = ExchangeTransaction

  implicit val validator = ExchangeTxValidator
  val serializer         = ExchangeTxSerializer

  implicit def sign(tx: ExchangeTransaction, privateKey: PrivateKey): ExchangeTransaction =
    tx.copy(proofs = Proofs(crypto.sign(privateKey, tx.bodyBytes())))

  override def parseBytes(bytes: Array[TxVersion]): Try[ExchangeTransaction] =
    serializer.parseBytes(bytes)

  override def supportedVersions: Set[TxVersion] = Set(1, 2, 3)

  val typeId: TxType = 7: Byte

  def create(
      version: TxVersion,
      order1: Order,
      order2: Order,
      amount: Long,
      price: Long,
      buyMatcherFee: Long,
      sellMatcherFee: Long,
      fee: Long,
      timestamp: Long,
      proofs: Proofs = Proofs.empty,
      chainId: Byte = AddressScheme.current.chainId
  ): Either[ValidationError, ExchangeTransaction] =
    ExchangeTransaction(version, order1, order2, amount, price, buyMatcherFee, sellMatcherFee, fee, timestamp, proofs, chainId).validatedEither

  def signed(
      version: TxVersion,
      matcher: PrivateKey,
      order1: Order,
      order2: Order,
      amount: Long,
      price: Long,
      buyMatcherFee: Long,
      sellMatcherFee: Long,
      fee: Long,
      timestamp: Long
  ): Either[ValidationError, ExchangeTransaction] =
    create(version, order1, order2, amount, price, buyMatcherFee, sellMatcherFee, fee, timestamp, Proofs.empty).map(_.signWith(matcher))
}
