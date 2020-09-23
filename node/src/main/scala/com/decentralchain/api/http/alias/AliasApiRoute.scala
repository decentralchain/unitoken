package com.decentralchain.api.http.alias

import akka.NotUsed
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import cats.syntax.either._
import com.decentralchain.account.Alias
import com.decentralchain.api.common.CommonTransactionsApi
import com.decentralchain.api.http._
import com.decentralchain.api.http.requests.CreateAliasRequest
import com.decentralchain.http.BroadcastRoute
import com.decentralchain.network.UtxPoolSynchronizer
import com.decentralchain.settings.RestAPISettings
import com.decentralchain.state.Blockchain
import com.decentralchain.transaction._
import com.decentralchain.utils.Time
import com.decentralchain.wallet.Wallet
import play.api.libs.json.{JsString, JsValue, Json}

case class AliasApiRoute(
    settings: RestAPISettings,
    commonApi: CommonTransactionsApi,
    wallet: Wallet,
    utxPoolSynchronizer: UtxPoolSynchronizer,
    time: Time,
    blockchain: Blockchain
) extends ApiRoute
    with BroadcastRoute
    with AuthRoute {

  override val route: Route = pathPrefix("alias") {
    addressOfAlias ~ aliasOfAddress ~ deprecatedRoute
  }

  private def deprecatedRoute: Route =
    path("broadcast" / "create") {
      broadcast[CreateAliasRequest](_.toTx)
    } ~ (path("create") & withAuth) {
      broadcast[CreateAliasRequest](TransactionFactory.createAlias(_, wallet, time))
    }

  def addressOfAlias: Route = (get & path("by-alias" / Segment)) { aliasName =>
    complete {
      Alias
        .create(aliasName)
        .flatMap { a =>
          blockchain.resolveAlias(a).bimap(_ => TxValidationError.AliasDoesNotExist(a), addr => Json.obj("address" -> addr.stringRepr))
        }
    }
  }

  private implicit val ess: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  def aliasOfAddress: Route = (get & path("by-address" / AddrSegment)) { address =>
    extractScheduler { implicit s =>
      val value: Source[JsValue, NotUsed] =
        Source.fromPublisher(commonApi.aliasesOfAddress(address).map { case (_, tx) => JsString(tx.alias.stringRepr) }.toReactivePublisher)
      complete(value)
    }
  }
}
