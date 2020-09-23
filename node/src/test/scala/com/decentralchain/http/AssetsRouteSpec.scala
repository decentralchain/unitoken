package com.decentralchain.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.decentralchain.account.Address
import com.decentralchain.api.common.{CommonAccountsApi, CommonAssetsApi}
import com.decentralchain.api.http.assets.AssetsApiRoute
import com.decentralchain.api.http.requests.{TransferV1Request, TransferV2Request}
import com.decentralchain.http.ApiMarshallers._
import com.decentralchain.it.util.DoubleExt
import com.decentralchain.state.Blockchain
import com.decentralchain.transaction.transfer._
import com.decentralchain.wallet.Wallet
import com.decentralchain.{RequestGen, TestTime}
import org.scalamock.scalatest.PathMockFactory
import org.scalatest.concurrent.Eventually
import play.api.libs.json.Writes

class AssetsRouteSpec extends RouteSpec("/assets") with RequestGen with PathMockFactory with Eventually with RestAPISettingsHelper {

  private val wallet = stub[Wallet]
  private val state  = stub[Blockchain]

  private val seed               = "seed".getBytes("UTF-8")
  private val senderPrivateKey   = Wallet.generateNewAccount(seed, 0)
  private val receiverPrivateKey = Wallet.generateNewAccount(seed, 1)

  (wallet.privateKeyAccount _).when(senderPrivateKey.toAddress).onCall((_: Address) => Right(senderPrivateKey)).anyNumberOfTimes()

  "/transfer" - {
    val route: Route = AssetsApiRoute(
      restAPISettings,
      wallet,
      DummyUtxPoolSynchronizer.accepting,
      state,
      new TestTime(),
      mock[CommonAccountsApi],
      mock[CommonAssetsApi]
    ).route

    def posting[A: Writes](v: A): RouteTestResult = Post(routePath("/transfer"), v).addHeader(ApiKeyHeader) ~> route

    "accepts TransferRequest" in {
      val req = TransferV1Request(
        assetId = None,
        feeAssetId = None,
        amount = 1.unitoken,
        fee = 0.3.unitoken,
        sender = senderPrivateKey.toAddress.toString,
        attachment = Some("attachment"),
        recipient = receiverPrivateKey.toAddress.toString,
        timestamp = Some(System.currentTimeMillis())
      )

      posting(req) ~> check {
        status shouldBe StatusCodes.OK

        responseAs[TransferTransaction]
      }
    }

    "accepts VersionedTransferRequest" in {
      val req = TransferV2Request(
        assetId = None,
        amount = 1.unitoken,
        feeAssetId = None,
        fee = 0.3.unitoken,
        sender = senderPrivateKey.toAddress.toString,
        attachment = None,
        recipient = receiverPrivateKey.toAddress.toString,
        timestamp = Some(System.currentTimeMillis())
      )

      posting(req) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[TransferV2Request]
      }
    }

    "returns a error if it is not a transfer request" in {
      val req = issueReq.sample.get
      posting(req) ~> check {
        status shouldNot be(StatusCodes.OK)
      }
    }
  }

}
