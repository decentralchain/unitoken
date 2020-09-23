package com.decentralchain.api.common

import com.decentralchain.account.Address
import com.decentralchain.api.common.CommonAssetsApi.AssetInfo
import com.decentralchain.crypto
import com.decentralchain.database.{AddressId, KeyTags}
import com.decentralchain.state.{AssetDescription, Blockchain, Diff, Portfolio}
import com.decentralchain.transaction.Asset.IssuedAsset
import com.decentralchain.transaction.assets.IssueTransaction
import monix.reactive.Observable
import org.iq80.leveldb.DB

trait CommonAssetsApi {
  def description(assetId: IssuedAsset): Option[AssetDescription]

  def fullInfo(assetId: IssuedAsset): Option[AssetInfo]

  def unitokenDistribution(height: Int, after: Option[Address]): Observable[(Address, Long)]

  def assetDistribution(asset: IssuedAsset, height: Int, after: Option[Address]): Observable[(Address, Long)]
}

object CommonAssetsApi {
  final case class AssetInfo(description: AssetDescription, issueTransaction: Option[IssueTransaction], sponsorBalance: Option[Long])

  def apply(diff: => Diff, db: DB, blockchain: Blockchain): CommonAssetsApi = new CommonAssetsApi {
    def description(assetId: IssuedAsset): Option[AssetDescription] = {
      blockchain.assetDescription(assetId)
    }

    def fullInfo(assetId: IssuedAsset): Option[AssetInfo] =
      for {
        assetInfo <- blockchain.assetDescription(assetId)
        sponsorBalance = if (assetInfo.sponsorship != 0) Some(blockchain.unitokenPortfolio(assetInfo.issuer.toAddress).spendableBalance) else None
      } yield AssetInfo(assetInfo, blockchain.transactionInfo(assetId.id).collect { case (_, it: IssueTransaction, true) => it }, sponsorBalance)

    override def unitokenDistribution(height: Int, after: Option[Address]): Observable[(Address, Long)] =
      balanceDistribution(
        db,
        height,
        after,
        if (height == blockchain.height) diff.portfolios else Map.empty[Address, Portfolio],
        KeyTags.unitokenBalance.prefixBytes,
        bs => AddressId.fromByteArray(bs.slice(2, bs.length - 4)),
        _.balance
      )

    override def assetDistribution(asset: IssuedAsset, height: Int, after: Option[Address]): Observable[(Address, Long)] =
      balanceDistribution(
        db,
        height,
        after,
        if (height == blockchain.height) diff.portfolios else Map.empty[Address, Portfolio],
        KeyTags.AssetBalance.prefixBytes ++ asset.id.arr,
        bs => AddressId.fromByteArray(bs.slice(2 + crypto.DigestLength, bs.length - 4)),
        _.assets.getOrElse(asset, 0L)
      )
  }
}
