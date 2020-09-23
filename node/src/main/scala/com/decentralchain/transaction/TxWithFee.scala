package com.decentralchain.transaction

import com.decentralchain.transaction.Asset.unitoken

sealed trait TxWithFee {
  def fee: TxAmount
  def assetFee: (Asset, TxAmount) // TODO: Delete or rework
}

object TxWithFee {
  trait Inunitoken extends TxWithFee {
    override def assetFee: (Asset, TxAmount) = (unitoken, fee)
  }

  trait InCustomAsset extends TxWithFee {
    def feeAssetId: Asset
    override def assetFee: (Asset, TxAmount) = (feeAssetId, fee)
  }
}
