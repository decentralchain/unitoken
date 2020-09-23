package com.decentralchain.features

import com.decentralchain.state.Blockchain

object MultiPaymentPolicyProvider {
  implicit class MultiPaymentAllowedExt(b: Blockchain) {
    val allowsMultiPayment: Boolean =
      b.activatedFeatures.contains(BlockchainFeatures.BlockV5.id)
  }
}
