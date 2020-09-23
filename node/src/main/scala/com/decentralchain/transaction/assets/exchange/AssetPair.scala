package com.decentralchain.transaction.assets.exchange

import com.google.common.primitives.Bytes
import com.decentralchain.common.state.ByteStr
import com.decentralchain.serialization.Deser
import com.decentralchain.transaction.Asset.{IssuedAsset, unitoken}
import com.decentralchain.transaction._
import com.decentralchain.transaction.assets.exchange.Validation.booleanOperators
import net.ceedubs.ficus.readers.ValueReader
import play.api.libs.json.{JsObject, Json}

import scala.util.{Failure, Success, Try}


case class AssetPair(
     amountAsset: Asset,
     priceAsset: Asset
) {
  import AssetPair._

  lazy val priceAssetStr: String  = assetIdStr(priceAsset)
  lazy val amountAssetStr: String = assetIdStr(amountAsset)
  override def toString: String   = key
  def key: String                 = amountAssetStr + "-" + priceAssetStr
  def isValid: Validation         = (amountAsset != priceAsset) :| "Invalid AssetPair"
  def bytes: Array[Byte]          = Bytes.concat(amountAsset.byteRepr, priceAsset.byteRepr)
  def json: JsObject = Json.obj(
    "amountAsset" -> amountAsset.maybeBase58Repr,
    "priceAsset"  -> priceAsset.maybeBase58Repr
  )
  def reverse = AssetPair(priceAsset, amountAsset)

  def assets: Set[Asset] = Set(amountAsset, priceAsset)
}

object AssetPair {
  val unitokenName = "unitoken"

  def assetIdStr(aid: Asset): String = aid match {
    case unitoken           => unitokenName
    case IssuedAsset(id) => id.toString
  }

  def extractAssetId(a: String): Try[Asset] = a match {
    case `unitokenName` => Success(unitoken)
    case other       => ByteStr.decodeBase58(other).map(IssuedAsset)
  }

  def createAssetPair(amountAsset: String, priceAsset: String): Try[AssetPair] =
    for {
      a1 <- extractAssetId(amountAsset)
      a2 <- extractAssetId(priceAsset)
    } yield AssetPair(a1, a2)

  def fromBytes(xs: Array[Byte]): AssetPair = {
    val (amount, offset) = Deser.parseByteArrayOption(xs, 0, AssetIdLength)
    val (price, _)       = Deser.parseByteArrayOption(xs, offset, AssetIdLength)
    AssetPair(
      Asset.fromCompatId(amount.map(ByteStr(_))),
      Asset.fromCompatId(price.map(ByteStr(_)))
    )
  }

  def fromString(s: String): Try[AssetPair] = Try(s.split("-")).flatMap {
    case Array(amtAssetStr, prcAssetStr) => AssetPair.createAssetPair(amtAssetStr, prcAssetStr)
    case xs                              => Failure(new Exception(s"$s (incorrect assets count, expected 2 but got ${xs.size}: ${xs.mkString(", ")})"))
  }

  implicit val assetPairReader: ValueReader[AssetPair] = (cfg, path) => fromString(cfg.getString(path)).get
}
