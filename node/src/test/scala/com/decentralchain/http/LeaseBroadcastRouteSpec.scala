package com.decentralchain.http

import com.decentralchain.api.common.CommonAccountsApi
import com.decentralchain.api.http.ApiError._
import com.decentralchain.api.http._
import com.decentralchain.api.http.leasing.LeaseApiRoute
import com.decentralchain.state.Blockchain
import com.decentralchain.state.diffs.TransactionDiffer.TransactionValidationError
import com.decentralchain.transaction.Transaction
import com.decentralchain.transaction.TxValidationError.GenericError
import com.decentralchain.transaction.lease.LeaseCancelTransaction
import com.decentralchain.utils.Time
import com.decentralchain.wallet.Wallet
import com.decentralchain.{NoShrink, RequestGen}
import org.scalacheck.Gen.posNum
import org.scalacheck.{Gen => G}
import org.scalamock.scalatest.PathMockFactory
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}
import play.api.libs.json.Json._
import play.api.libs.json._

class LeaseBroadcastRouteSpec
    extends RouteSpec("/leasing/broadcast/")
    with RequestGen
    with PathMockFactory
    with PropertyChecks
    with RestAPISettingsHelper
    with NoShrink {
  private[this] val utxPoolSynchronizer = DummyUtxPoolSynchronizer.rejecting(t => TransactionValidationError(GenericError("foo"), t))
  private[this] val route               = LeaseApiRoute(restAPISettings, stub[Wallet], stub[Blockchain], utxPoolSynchronizer, stub[Time], stub[CommonAccountsApi]).route
  "returns StateCheckFailed" - {

    val vt = Table[String, G[_ <: Transaction], JsValue => JsValue](
      ("url", "generator", "transform"),
      ("lease", leaseGen.retryUntil(_.version == 1), identity),
      ("cancel", leaseCancelGen.retryUntil(_.isInstanceOf[LeaseCancelTransaction]), {
        case o: JsObject => o ++ Json.obj("txId" -> o.value("leaseId"))
        case other       => other
      })
    )

    def posting(url: String, v: JsValue): RouteTestResult = Post(routePath(url), v) ~> route

    "when state validation fails" in {
      forAll(vt) { (url, gen, transform) =>
        forAll(gen) { t: Transaction =>
          posting(url, transform(t.json())) should produce(StateCheckFailed(t, "foo"))
        }
      }
    }
  }

  "returns appropriate error code when validation fails for" - {

    "lease transaction" in forAll(leaseReq) { lease =>
      def posting[A: Writes](v: A): RouteTestResult = Post(routePath("lease"), v) ~> route

      forAll(nonPositiveLong) { q =>
        posting(lease.copy(amount = q)) should produce(NonPositiveAmount(s"$q of unitoken"))
      }
      forAll(invalidBase58) { pk =>
        posting(lease.copy(senderPublicKey = pk)) should produce(InvalidAddress)
      }
      forAll(invalidBase58) { a =>
        posting(lease.copy(recipient = a)) should produce(InvalidAddress)
      }
      forAll(nonPositiveLong) { fee =>
        posting(lease.copy(fee = fee)) should produce(InsufficientFee())
      }
      forAll(posNum[Long]) { quantity =>
        posting(lease.copy(amount = quantity, fee = Long.MaxValue)) should produce(OverflowError)
      }
    }

    "lease cancel transaction" in forAll(leaseCancelReq) { cancel =>
      def posting[A: Writes](v: A): RouteTestResult = Post(routePath("cancel"), v) ~> route

      forAll(invalidBase58) { pk =>
        posting(cancel.copy(leaseId = pk)) should produce(CustomValidationError("invalid.leaseTx"))
      }
      forAll(invalidBase58) { pk =>
        posting(cancel.copy(senderPublicKey = pk)) should produce(InvalidAddress)
      }
      forAll(nonPositiveLong) { fee =>
        posting(cancel.copy(fee = fee)) should produce(InsufficientFee())
      }
    }
  }
}
