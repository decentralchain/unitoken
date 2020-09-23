package com.decentralchain.transaction.validation.impl

import com.decentralchain.transaction.Asset.IssuedAsset
import com.decentralchain.transaction.assets.UpdateAssetInfoTransaction
import com.decentralchain.transaction.validation.{TxValidator, ValidatedV}
import com.decentralchain.transaction.{Asset, TxValidationError}
import com.decentralchain.utils.StringBytes

object UpdateAssetInfoTxValidator extends TxValidator[UpdateAssetInfoTransaction] {
  override def validate(tx: UpdateAssetInfoTransaction): ValidatedV[UpdateAssetInfoTransaction] =
    V.seq(tx)(
      V.cond(UpdateAssetInfoTransaction.supportedVersions(tx.version), TxValidationError.UnsupportedVersion(tx.version)),
      V.fee(tx.feeAmount),
      V.asset[IssuedAsset](tx.assetId),
      V.asset[Asset](tx.feeAsset),
      V.assetName(tx.name.toByteString),
      V.assetDescription(tx.description.toByteString)
    )
}
