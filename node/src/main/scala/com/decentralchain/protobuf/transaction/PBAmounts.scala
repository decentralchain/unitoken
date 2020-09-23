package com.decentralchain.protobuf.transaction
import com.google.protobuf.ByteString
import com.decentralchain.protobuf.Amount
import com.decentralchain.transaction.Asset
import com.decentralchain.transaction.Asset.{IssuedAsset, unitoken}
import com.decentralchain.protobuf.utils.PBImplicitConversions._

object PBAmounts {
  def toPBAssetId(asset: Asset): ByteString = asset match {
    case Asset.IssuedAsset(id) => id.toByteString
    case Asset.unitoken           => ByteString.EMPTY
  }

  def toVanillaAssetId(byteStr: ByteString): Asset = {
    if (byteStr.isEmpty) unitoken
    else IssuedAsset(byteStr.toByteStr)
  }

  def fromAssetAndAmount(asset: Asset, amount: Long): Amount =
    Amount(toPBAssetId(asset), amount)

  def toAssetAndAmount(value: Amount): (Asset, Long) =
    (toVanillaAssetId(value.assetId), value.amount)
}
