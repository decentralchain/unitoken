package com.decentralchain.api.http

import akka.http.scaladsl.server._
import com.decentralchain.api.http.ApiError.ApiKeyNotValid
import com.decentralchain.common.utils.Base58
import com.decentralchain.crypto
import com.decentralchain.http.{ApiMarshallers, `X-Api-Key`, api_key}
import com.decentralchain.settings.RestAPISettings
import com.decentralchain.utils._

trait ApiRoute extends Directives with CustomDirectives with ApiMarshallers with ScorexLogging {
  def route: Route
}

trait AuthRoute { this: ApiRoute =>
  def settings: RestAPISettings

  protected lazy val apiKeyHash: Option[Array[Byte]] = Base58.tryDecode(settings.apiKeyHash).toOption

  def withAuth: Directive0 = apiKeyHash.fold[Directive0](complete(ApiKeyNotValid)) { hashFromSettings =>
    optionalHeaderValueByType[`X-Api-Key`](()).flatMap {
      case Some(k) if java.util.Arrays.equals(crypto.secureHash(k.value.utf8Bytes), hashFromSettings) => pass
      case _ =>
        optionalHeaderValueByType[api_key](()).flatMap {
          case Some(k) if java.util.Arrays.equals(crypto.secureHash(k.value.utf8Bytes), hashFromSettings) => pass
          case _                                                                                                  => complete(ApiKeyNotValid)
        }
    }
  }
}
