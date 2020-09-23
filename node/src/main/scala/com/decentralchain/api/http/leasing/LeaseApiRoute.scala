package com.decentralchain.api.http.leasing

import akka.http.scaladsl.server.Route
import com.decentralchain.api.common.CommonAccountsApi
import com.decentralchain.api.http._
import com.decentralchain.api.http.requests.{LeaseCancelRequest, LeaseRequest}
import com.decentralchain.http.BroadcastRoute
import com.decentralchain.network.UtxPoolSynchronizer
import com.decentralchain.settings.RestAPISettings
import com.decentralchain.state.Blockchain
import com.decentralchain.transaction._
import com.decentralchain.transaction.lease.LeaseTransaction
import com.decentralchain.utils.Time
import com.decentralchain.wallet.Wallet
import play.api.libs.json.JsNumber

case class LeaseApiRoute(
    settings: RestAPISettings,
    wallet: Wallet,
    blockchain: Blockchain,
    utxPoolSynchronizer: UtxPoolSynchronizer,
    time: Time,
    commonAccountApi: CommonAccountsApi
) extends ApiRoute
    with BroadcastRoute
    with AuthRoute {

  override val route: Route = pathPrefix("leasing") {
    active ~ deprecatedRoute
  }

  private def deprecatedRoute: Route =
    (path("lease") & withAuth) {
      broadcast[LeaseRequest](TransactionFactory.lease(_, wallet, time))
    } ~ (path("cancel") & withAuth) {
      broadcast[LeaseCancelRequest](TransactionFactory.leaseCancel(_, wallet, time))
    } ~ pathPrefix("broadcast") {
      path("lease")(broadcast[LeaseRequest](_.toTx)) ~
        path("cancel")(broadcast[LeaseCancelRequest](_.toTx))
    }

  def active: Route = (pathPrefix("active") & get & extractScheduler) { implicit sc =>
    path(AddrSegment) { address =>
      complete(
        commonAccountApi
          .activeLeases(address)
          .collect {
            case (height, leaseTransaction: LeaseTransaction) =>
              leaseTransaction.json() + ("height" -> JsNumber(height))
          }
          .toListL
          .runToFuture
      )
    }
  }
}
