package com.decentralchain.api.http.requests

import com.decentralchain.account.PublicKey
import com.decentralchain.lang.ValidationError
import com.decentralchain.transaction.Asset.IssuedAsset
import com.decentralchain.transaction.assets.SponsorFeeTransaction
import com.decentralchain.transaction.{AssetIdStringLength, Proofs}
import play.api.libs.json.{Format, Json}

object SponsorFeeRequest {
  implicit val unsignedSponsorRequestFormat: Format[SponsorFeeRequest]     = Json.format
  implicit val signedSponsorRequestFormat: Format[SignedSponsorFeeRequest] = Json.format
}

case class SponsorFeeRequest(
    version: Option[Byte],
    sender: String,
    assetId: String,
    minSponsoredAssetFee: Option[Long],
    fee: Long,
    timestamp: Option[Long] = None
)

case class SignedSponsorFeeRequest(
    version: Option[Byte],
    senderPublicKey: String,
    assetId: String,
    minSponsoredAssetFee: Option[Long],
    fee: Long,
    timestamp: Long,
    proofs: Proofs
) {
  def toTx: Either[ValidationError, SponsorFeeTransaction] =
    for {
      _sender <- PublicKey.fromBase58String(senderPublicKey)
      _asset  <- parseBase58(assetId, "invalid.assetId", AssetIdStringLength).map(IssuedAsset)
      t       <- SponsorFeeTransaction.create(version.getOrElse(1.toByte), _sender, _asset, minSponsoredAssetFee.filterNot(_ == 0), fee, timestamp, proofs)
    } yield t
}
