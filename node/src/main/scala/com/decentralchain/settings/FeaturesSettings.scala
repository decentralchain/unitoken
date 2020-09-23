package com.decentralchain.settings

case class FeaturesSettings(autoShutdownOnUnsupportedFeature: Boolean, supported: List[Short] = List.empty)
