package com.decentralchain.db
import com.typesafe.config.ConfigFactory
import com.decentralchain.settings.unitokenSettings

trait DBCacheSettings {
  lazy val dbSettings = unitokenSettings.fromRootConfig(ConfigFactory.load()).dbSettings
  lazy val maxCacheSize: Int = dbSettings.maxCacheSize
}
