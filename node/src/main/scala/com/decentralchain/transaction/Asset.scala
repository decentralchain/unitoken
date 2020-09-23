package com.decentralchain.transaction

import com.decentralchain.common.state.ByteStr
import com.decentralchain.common.utils.Base58
import com.decentralchain.transaction.assets.exchange.AssetPair
import net.ceedubs.ficus.readers.ValueReader
import play.api.libs.json._

import scala.util.Success

sealed trait Asset
object Asset {
  final case class IssuedAsset(id: ByteStr) extends Asset
  case object unitoken                         extends Asset

  implicit val assetReads: Reads[IssuedAsset] = Reads {
    case JsString(str) if str.length > AssetIdStringLength =>
      JsError(s"Too long assetId: length of $str exceeds $AssetIdStringLength")
    case JsString(str) =>
      Base58.tryDecodeWithLimit(str) match {
        case Success(arr) => JsSuccess(IssuedAsset(ByteStr(arr)))
        case _            => JsError("Expected base58-encoded assetId")
      }
    case _ => JsError("Expected base58-encoded assetId")
  }
  implicit val assetWrites: Writes[IssuedAsset] = Writes { asset =>
    JsString(asset.id.toString)
  }

  implicit val assetIdReads: Reads[Asset] = Reads {
    case json: JsString => if (json.value.isEmpty) JsSuccess(unitoken) else assetReads.reads(json)
    case JsNull         => JsSuccess(unitoken)
    case _              => JsError("Expected base58-encoded assetId or null")
  }
  implicit val assetIdWrites: Writes[Asset] = Writes {
    case unitoken           => JsNull
    case IssuedAsset(id) => JsString(id.toString)
  }

  object Formats {
    implicit val assetJsonFormat: Format[IssuedAsset] = Format(assetReads, assetWrites)
    implicit val assetIdJsonFormat: Format[Asset]     = Format(assetIdReads, assetIdWrites)
  }

  implicit val assetReader: ValueReader[Asset] = { (cfg, path) =>
    AssetPair.extractAssetId(cfg getString path).fold(ex => throw new Exception(ex.getMessage), identity)
  }

  def fromString(maybeStr: Option[String]): Asset = {
    maybeStr.map(x => IssuedAsset(ByteStr.decodeBase58(x).get)).getOrElse(unitoken)
  }

  def fromCompatId(maybeBStr: Option[ByteStr]): Asset = {
    maybeBStr.fold[Asset](unitoken)(IssuedAsset)
  }

  implicit class AssetIdOps(private val ai: Asset) extends AnyVal {
    def byteRepr: Array[Byte] = ai match {
      case unitoken           => Array(0: Byte)
      case IssuedAsset(id) => (1: Byte) +: id.arr
    }

    def compatId: Option[ByteStr] = ai match {
      case unitoken           => None
      case IssuedAsset(id) => Some(id)
    }

    def maybeBase58Repr: Option[String] = ai match {
      case unitoken           => None
      case IssuedAsset(id) => Some(id.toString)
    }

    def fold[A](onunitoken: => A)(onAsset: IssuedAsset => A): A = ai match {
      case unitoken                  => onunitoken
      case asset @ IssuedAsset(_) => onAsset(asset)
    }
  }
}
