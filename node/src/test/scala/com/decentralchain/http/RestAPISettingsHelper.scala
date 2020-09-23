package com.decentralchain.http

import com.typesafe.config.ConfigFactory
import com.decentralchain.common.utils.Base58
import com.decentralchain.crypto
import com.decentralchain.settings._
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

trait RestAPISettingsHelper {
  private val apiKey: String = "test_api_key"

  val ApiKeyHeader = `X-Api-Key`(apiKey)

  lazy val MaxTransactionsPerRequest = 10000
  lazy val MaxAddressesPerRequest    = 10000

  lazy val restAPISettings = {
    val keyHash = Base58.encode(crypto.secureHash(apiKey.getBytes("UTF-8")))
    ConfigFactory
      .parseString(
        s"""unitoken.rest-api {
           |  api-key-hash = $keyHash
           |  transactions-by-address-limit = $MaxTransactionsPerRequest
           |  distribution-by-address-limit = $MaxAddressesPerRequest
           |}
         """.stripMargin
      )
      .withFallback(ConfigFactory.load())
      .as[RestAPISettings]("unitoken.rest-api")
  }
}
