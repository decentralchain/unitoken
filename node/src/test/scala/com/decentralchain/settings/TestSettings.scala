package com.decentralchain.settings

import com.typesafe.config.ConfigFactory
import com.decentralchain.features.BlockchainFeatures

object TestSettings {
  val Default: unitokenSettings = unitokenSettings.fromRootConfig(ConfigFactory.load())

  implicit class unitokenSettingsExt(val ws: unitokenSettings) extends AnyVal {
    def withFunctionalitySettings(fs: FunctionalitySettings): unitokenSettings =
      ws.copy(blockchainSettings = ws.blockchainSettings.copy(functionalitySettings = fs))

    def withNG: unitokenSettings =
      ws.withFunctionalitySettings(
        ws.blockchainSettings.functionalitySettings.copy(
          blockVersion3AfterHeight = 0,
          preActivatedFeatures = ws.blockchainSettings.functionalitySettings.preActivatedFeatures ++ Map(BlockchainFeatures.NG.id -> 0)
        )
      )
  }
}
