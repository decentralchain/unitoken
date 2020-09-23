package com.decentralchain.transaction.validation.impl

import cats.implicits._
import com.decentralchain.transaction.TxAmount
import com.decentralchain.transaction.TxValidationError.NegativeMinFee
import com.decentralchain.transaction.assets.SponsorFeeTransaction
import com.decentralchain.transaction.validation.{TxValidator, ValidatedV}

object SponsorFeeTxValidator extends TxValidator[SponsorFeeTransaction] {
  override def validate(tx: SponsorFeeTransaction): ValidatedV[SponsorFeeTransaction] = {
    import tx._
    V.seq(tx)(
      checkMinSponsoredAssetFee(minSponsoredAssetFee).toValidatedNel,
      V.fee(fee)
    )
  }

  def checkMinSponsoredAssetFee(minSponsoredAssetFee: Option[TxAmount]): Either[NegativeMinFee, Unit] =
    Either.cond(minSponsoredAssetFee.forall(_ > 0), (), NegativeMinFee(minSponsoredAssetFee.get, "asset"))
}
