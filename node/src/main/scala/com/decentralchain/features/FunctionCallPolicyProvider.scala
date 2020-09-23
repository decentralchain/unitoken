package com.decentralchain.features

import com.decentralchain.state.Blockchain

object FunctionCallPolicyProvider {
  implicit class MultiPaymentAllowedExt(b: Blockchain) {
    val callableListArgumentsAllowed: Boolean =
      b.activatedFeatures.contains(BlockchainFeatures.BlockV5.id)
  }
}
