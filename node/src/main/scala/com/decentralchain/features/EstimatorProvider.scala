package com.decentralchain.features

import com.decentralchain.features.BlockchainFeatures.{BlockReward, BlockV5}
import com.decentralchain.lang.v1.estimator.v2.ScriptEstimatorV2
import com.decentralchain.lang.v1.estimator.v3.ScriptEstimatorV3
import com.decentralchain.lang.v1.estimator.{ScriptEstimator, ScriptEstimatorV1}
import com.decentralchain.settings.unitokenSettings
import com.decentralchain.state.Blockchain

object EstimatorProvider {
  implicit class EstimatorBlockchainExt(b: Blockchain) {
    val estimator: ScriptEstimator =
      if (b.isFeatureActivated(BlockV5)) ScriptEstimatorV3
      else if (b.isFeatureActivated(BlockReward)) ScriptEstimatorV2
      else ScriptEstimatorV1
  }

  implicit class EstimatorunitokenSettingsExt(ws: unitokenSettings) {
    val estimator: ScriptEstimator =
      if (ws.featuresSettings.supported.contains(BlockV5.id)) ScriptEstimatorV3
      else if (ws.featuresSettings.supported.contains(BlockReward.id)) ScriptEstimatorV2
      else ScriptEstimatorV1
  }
}
