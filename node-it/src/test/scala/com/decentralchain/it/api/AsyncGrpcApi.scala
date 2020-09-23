package com.decentralchain.it.api

import java.util.NoSuchElementException

import com.google.protobuf.ByteString
import com.google.protobuf.empty.Empty
import com.decentralchain.account.{AddressScheme, Alias, KeyPair}
import com.decentralchain.api.grpc.BalanceResponse.unitokenBalances
import com.decentralchain.api.grpc.{TransactionStatus => PBTransactionStatus, _}
import com.decentralchain.common.utils.{Base58, EitherExt2}
import com.decentralchain.crypto
import com.decentralchain.it.Node
import com.decentralchain.it.util.GlobalTimer.{instance => timer}
import com.decentralchain.it.util._
import com.decentralchain.lang.script.{Script => Scr}
import com.decentralchain.lang.v1.Serde
import com.decentralchain.lang.v1.compiler.Terms.FUNCTION_CALL
import com.decentralchain.protobuf.Amount
import com.decentralchain.protobuf.block.PBBlocks
import com.decentralchain.serialization.Deser
import com.decentralchain.transaction.Asset.unitoken
import com.decentralchain.transaction.assets.exchange.Order
import com.decentralchain.transaction.{Asset, TxVersion}
import io.grpc.stub.StreamObserver
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.subjects.ConcurrentSubject
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.duration._

object AsyncGrpcApi {
  implicit class NodeAsyncGrpcApi(val n: Node) {

    import com.decentralchain.protobuf.transaction.{Transaction => PBTransaction, _}
    import monix.execution.Scheduler.Implicits.global

    private[this] lazy val assets       = AssetsApiGrpc.stub(n.grpcChannel)
    private[this] lazy val accounts     = AccountsApiGrpc.stub(n.grpcChannel)
    private[this] lazy val blocks       = BlocksApiGrpc.stub(n.grpcChannel)
    private[this] lazy val transactions = TransactionsApiGrpc.stub(n.grpcChannel)

    val chainId: Byte = AddressScheme.current.chainId

    def blockAt(height: Int): Future[Block] = {
      blocks
        .getBlock(BlockRequest.of(includeTransactions = true, BlockRequest.Request.Height(height)))
        .map(r => PBBlocks.vanilla(r.getBlock).get.json().as[Block])
    }

    def stateChanges(
        request: TransactionsRequest
    ): Future[Seq[(com.decentralchain.transaction.Transaction, StateChangesDetails)]] = {
      val (obs, result) = createCallObserver[InvokeScriptResultResponse]
      transactions.getStateChanges(request, obs)
      result.runToFuture.map { r =>
        import com.decentralchain.state.{InvokeScriptResult => VISR}
        r.map { r =>
          val tx = PBTransactions.vanillaUnsafe(r.getTransaction)
          assert(r.getResult.transfers.forall(_.address.size() == 20))
          val result = Json.toJson(VISR.fromPB(r.getResult)).as[StateChangesDetails]
          (tx, result)
        }
      }
    }

    def stateChanges(
        txIds: Seq[String] = Nil,
        address: ByteString = ByteString.EMPTY
    ): Future[Seq[(com.decentralchain.transaction.Transaction, StateChangesDetails)]] = {
      val ids = txIds.map(id => ByteString.copyFrom(Base58.decode(id)))
      stateChanges(TransactionsRequest().addTransactionIds(ids: _*).withSender(address))
    }

    def broadcastIssue(
        source: KeyPair,
        name: String,
        quantity: Long,
        decimals: Int,
        reissuable: Boolean,
        fee: Long,
        description: String = "",
        script: Either[Array[Byte], Option[Scr]] = Right(None),
        version: Int = 2
    ): Future[PBSignedTransaction] = {
      val unsigned = PBTransaction(
        chainId,
        ByteString.copyFrom(source.publicKey.arr),
        Some(Amount.of(ByteString.EMPTY, fee)),
        System.currentTimeMillis(),
        version,
        PBTransaction.Data.Issue(IssueTransactionData.of(name, description, quantity, decimals, reissuable, toPBScript(script)))
      )

      script match {
        case Left(_) => transactions.broadcast(SignedTransaction.of(Some(unsigned), Seq(ByteString.EMPTY)))
        case _ =>
          val proofs =
            crypto.sign(source.privateKey, PBTransactions.vanilla(SignedTransaction(Some(unsigned)), unsafe = true).explicitGet().bodyBytes())
          transactions.broadcast(SignedTransaction.of(Some(unsigned), Seq(ByteString.copyFrom(proofs.arr))))
      }
    }

    def broadcastTransfer(
        source: KeyPair,
        recipient: Recipient,
        amount: Long,
        fee: Long,
        version: Int = 2,
        assetId: String = "unitoken",
        feeAssetId: String = "unitoken",
        attachment: ByteString = ByteString.EMPTY,
        timestamp: Long = System.currentTimeMillis
    ): Future[PBSignedTransaction] = {
      val unsigned = PBTransaction(
        chainId,
        ByteString.copyFrom(source.publicKey.arr),
        Some(Amount.of(if (feeAssetId == "unitoken") ByteString.EMPTY else ByteString.copyFrom(Base58.decode(feeAssetId)), fee)),
        timestamp,
        version,
        PBTransaction.Data.Transfer(
          TransferTransactionData.of(
            Some(recipient),
            Some(Amount.of(if (assetId == "unitoken") ByteString.EMPTY else ByteString.copyFrom(Base58.decode(assetId)), amount)),
            attachment
          )
        )
      )
      try {
        val proofs = crypto.sign(source.privateKey, PBTransactions.vanilla(SignedTransaction(Some(unsigned))).explicitGet().bodyBytes())
        transactions.broadcast(SignedTransaction.of(Some(unsigned), Seq(ByteString.copyFrom(proofs.arr))))
      } catch {
        case _: IllegalArgumentException => transactions.broadcast(SignedTransaction.of(Some(unsigned), Seq(ByteString.EMPTY)))
      }
    }

    def broadcastReissue(
        source: KeyPair,
        fee: Long,
        assetId: String,
        amount: Long,
        reissuable: Boolean = false,
        version: Int = 2
    ): Future[PBSignedTransaction] = {
      val unsigned = PBTransaction(
        chainId,
        ByteString.copyFrom(source.publicKey.arr),
        Some(Amount.of(ByteString.EMPTY, fee)),
        System.currentTimeMillis(),
        version,
        PBTransaction.Data.Reissue(
          ReissueTransactionData.of(
            Some(Amount.of(ByteString.copyFrom(Base58.decode(assetId)), amount)),
            reissuable
          )
        )
      )

      val proofs      = crypto.sign(source.privateKey, PBTransactions.vanilla(SignedTransaction(Some(unsigned))).explicitGet().bodyBytes())
      val transaction = SignedTransaction.of(Some(unsigned), Seq(ByteString.copyFrom(proofs.arr)))

      transactions.broadcast(transaction)
    }

    def broadcastCreateAlias(source: KeyPair, alias: String, fee: Long, version: Int = 2): Future[PBSignedTransaction] = {
      val unsigned = PBTransaction(
        chainId,
        ByteString.copyFrom(source.publicKey.arr),
        Some(Amount.of(ByteString.EMPTY, fee)),
        System.currentTimeMillis,
        version,
        PBTransaction.Data.CreateAlias(CreateAliasTransactionData(alias))
      )
      if (Alias.create(alias).isLeft) {
        transactions.broadcast(SignedTransaction.of(Some(unsigned), Seq(ByteString.EMPTY)))
      } else {
        val proofs = crypto.sign(source.privateKey, PBTransactions.vanilla(SignedTransaction(Some(unsigned))).explicitGet().bodyBytes())
        transactions.broadcast(SignedTransaction.of(Some(unsigned), Seq(ByteString.copyFrom(proofs.arr))))
      }
    }

    def putData(
        source: KeyPair,
        data: Seq[DataTransactionData.DataEntry],
        fee: Long,
        version: Int = 1,
        timestamp: Long = System.currentTimeMillis()
    ): Future[PBSignedTransaction] = {
      val unsigned = PBTransaction(
        chainId,
        ByteString.copyFrom(source.publicKey.arr),
        Some(Amount.of(ByteString.EMPTY, fee)),
        timestamp,
        version,
        PBTransaction.Data.DataTransaction(DataTransactionData.of(data))
      )
      if (PBTransactions.vanilla(SignedTransaction(Some(unsigned))).isLeft) {
        transactions.broadcast(SignedTransaction.of(Some(unsigned), Seq(ByteString.EMPTY)))
      } else {
        val proofs = crypto.sign(source.privateKey, PBTransactions.vanilla(SignedTransaction(Some(unsigned))).explicitGet().bodyBytes())
        transactions.broadcast(SignedTransaction.of(Some(unsigned), Seq(ByteString.copyFrom(proofs.arr))))
      }
    }

    def exchange(
        matcher: KeyPair,
        buyOrder: Order,
        sellOrder: Order,
        amount: Long,
        price: Long,
        buyMatcherFee: Long,
        sellMatcherFee: Long,
        fee: Long,
        timestamp: Long,
        version: Byte,
        matcherFeeAssetId: String = "unitoken"
    ): Future[PBSignedTransaction] = {

      val unsigned = PBTransaction(
        chainId,
        ByteString.copyFrom(matcher.publicKey.arr),
        Some(Amount.of(if (matcherFeeAssetId == "unitoken") ByteString.EMPTY else ByteString.copyFrom(Base58.decode(matcherFeeAssetId)), fee)),
        timestamp,
        version,
        PBTransaction.Data.Exchange(
          ExchangeTransactionData.of(
            amount,
            price,
            buyMatcherFee,
            sellMatcherFee,
            Seq(PBOrders.protobuf(buyOrder), PBOrders.protobuf(sellOrder))
          )
        )
      )

      val proofs      = crypto.sign(matcher.privateKey, PBTransactions.vanilla(SignedTransaction(Some(unsigned))).explicitGet().bodyBytes())
      val transaction = SignedTransaction.of(Some(unsigned), Seq(ByteString.copyFrom(proofs.arr)))

      transactions.broadcast(transaction)
    }

    private def toPBScript(v: Either[Array[Byte], Option[Scr]]): ByteString = v match {
      case Left(bytes) if bytes.length > 0 => ByteString.copyFrom(bytes)
      case Right(maybeScript)              => PBTransactions.toPBScript(maybeScript)
      case _                               => ByteString.EMPTY
    }

    def setScript(
        sender: KeyPair,
        script: Either[Array[Byte], Option[Scr]],
        fee: Long,
        timestamp: Long = System.currentTimeMillis(),
        version: Int = 1
    ): Future[PBSignedTransaction] = {
      val unsigned = PBTransaction(
        chainId,
        ByteString.copyFrom(sender.publicKey.arr),
        Some(Amount.of(ByteString.EMPTY, fee)),
        timestamp,
        version,
        PBTransaction.Data.SetScript(SetScriptTransactionData.of(toPBScript(script)))
      )

      script match {
        case Left(_) => transactions.broadcast(SignedTransaction.of(Some(unsigned), Seq(ByteString.EMPTY)))
        case _ =>
          val proofs =
            crypto.sign(sender.privateKey, PBTransactions.vanilla(SignedTransaction(Some(unsigned)), unsafe = true).explicitGet().bodyBytes())
          transactions.broadcast(SignedTransaction.of(Some(unsigned), Seq(ByteString.copyFrom(proofs.arr))))
      }
    }

    def getTransaction(id: String, sender: ByteString = ByteString.EMPTY, recipient: Option[Recipient] = None): Future[PBSignedTransaction] =
      getTransactionInfo(ByteString.copyFrom(Base58.decode(id)), sender, recipient).map(_.getTransaction)

    def getTransactionInfo(
        id: ByteString,
        sender: ByteString = ByteString.EMPTY,
        recipient: Option[Recipient] = None
    ): Future[TransactionResponse] = {
      val (obs, result) = createCallObserver[TransactionResponse]
      val req           = TransactionsRequest(transactionIds = Seq(id), sender = sender, recipient = recipient)
      transactions.getTransactions(req, obs)
      result.map(_.headOption.getOrElse(throw new NoSuchElementException("Transaction not found"))).runToFuture
    }

    def waitFor[A](desc: String)(f: this.type => Future[A], cond: A => Boolean, retryInterval: FiniteDuration): Future[A] = {
      n.log.debug(s"Awaiting condition '$desc'")
      timer
        .retryUntil(f(this), cond, retryInterval)
        .map(a => {
          n.log.debug(s"Condition '$desc' met")
          a
        })
    }

    def waitForTransaction(txId: String, retryInterval: FiniteDuration = 1.second): Future[PBSignedTransaction] = {
      val condition = waitFor[Option[PBSignedTransaction]](s"transaction $txId")(
        _.getTransaction(txId)
          .map(Option(_))
          .recover { case _: NoSuchElementException => None },
        tOpt => tOpt.exists(t => PBTransactions.vanilla(t).explicitGet().id().toString == txId),
        retryInterval
      ).map(_.get)

      condition
    }

    def height: Future[Int] = blocks.getCurrentHeight(Empty.of()).map(h => h.value)

    def waitForHeight(expectedHeight: Int): Future[Int] = {
      waitFor[Int](s"height >= $expectedHeight")(_.height, h => h >= expectedHeight, 5.seconds)
    }

    def unitokenBalance(address: ByteString): Future[unitokenBalances] = {
      val (obs, result) = createCallObserver[BalanceResponse]
      val req           = BalancesRequest.of(address, Seq(ByteString.EMPTY))
      accounts.getBalances(req, obs)
      result.map(_.headOption.getOrElse(throw new NoSuchElementException("Balances not found for address")).getunitoken).runToFuture
    }

    def broadcastBurn(source: KeyPair, assetId: String, amount: Long, fee: Long, version: Int = 2): Future[PBSignedTransaction] = {
      val unsigned = PBTransaction(
        chainId,
        ByteString.copyFrom(source.publicKey.arr),
        Some(Amount.of(ByteString.EMPTY, fee)),
        System.currentTimeMillis(),
        version,
        PBTransaction.Data.Burn(
          BurnTransactionData.of(
            Some(Amount.of(ByteString.copyFrom(Base58.decode(assetId)), amount))
          )
        )
      )

      val proofs      = crypto.sign(source.privateKey, PBTransactions.vanilla(SignedTransaction(Some(unsigned)), unsafe = true).explicitGet().bodyBytes())
      val transaction = SignedTransaction.of(Some(unsigned), Seq(ByteString.copyFrom(proofs.arr)))
      transactions.broadcast(transaction)
    }

    def broadcast(unsignedTx: PBTransaction, proofs: Seq[ByteString]): Future[PBSignedTransaction] =
      transactions.broadcast(SignedTransaction(Some(unsignedTx), proofs))

    def broadcastSponsorFee(sender: KeyPair, minFee: Option[Amount], fee: Long, version: Int = 1): Future[PBSignedTransaction] = {
      val unsigned = PBTransaction(
        chainId,
        ByteString.copyFrom(sender.publicKey.arr),
        Some(Amount.of(ByteString.EMPTY, fee)),
        System.currentTimeMillis,
        version,
        PBTransaction.Data.SponsorFee(SponsorFeeTransactionData.of(minFee))
      )
      val proofs = crypto.sign(sender.privateKey, PBTransactions.vanilla(SignedTransaction(Some(unsigned)), unsafe = true).explicitGet().bodyBytes())
      transactions.broadcast(SignedTransaction.of(Some(unsigned), Seq(ByteString.copyFrom(proofs.arr))))
    }

    def broadcastMassTransfer(
        sender: KeyPair,
        assetId: Option[String] = None,
        transfers: Seq[MassTransferTransactionData.Transfer],
        attachment: ByteString = ByteString.EMPTY,
        fee: Long,
        version: Int = 1
    ): Future[PBSignedTransaction] = {
      val unsigned = PBTransaction(
        chainId,
        ByteString.copyFrom(sender.publicKey.arr),
        Some(Amount.of(ByteString.EMPTY, fee)),
        System.currentTimeMillis(),
        version,
        PBTransaction.Data.MassTransfer(
          MassTransferTransactionData.of(
            if (assetId.isDefined) ByteString.copyFrom(Base58.decode(assetId.get)) else ByteString.EMPTY,
            transfers,
            attachment
          )
        )
      )
      val proofs = crypto.sign(sender.privateKey, PBTransactions.vanilla(SignedTransaction(Some(unsigned)), unsafe = true).explicitGet().bodyBytes())
      transactions.broadcast(SignedTransaction.of(Some(unsigned), Seq(ByteString.copyFrom(proofs.arr))))
    }

    def broadcastInvokeScript(
        caller: KeyPair,
        dApp: Recipient,
        functionCall: Option[FUNCTION_CALL],
        payments: Seq[Amount] = Seq.empty,
        fee: Long,
        version: Int = 2,
        feeAssetId: ByteString = ByteString.EMPTY
    ): Future[PBSignedTransaction] = {
      val unsigned = PBTransaction(
        chainId,
        ByteString.copyFrom(caller.publicKey.arr),
        Some(Amount.of(feeAssetId, fee)),
        System.currentTimeMillis,
        version,
        PBTransaction.Data.InvokeScript(
          InvokeScriptTransactionData(
            Some(dApp),
            ByteString.copyFrom(Deser.serializeOption(functionCall)(Serde.serialize(_))),
            payments
          )
        )
      )
      val proofs = crypto.sign(caller.privateKey, PBTransactions.vanilla(SignedTransaction(Some(unsigned)), unsafe = true).explicitGet().bodyBytes())
      transactions.broadcast(SignedTransaction.of(Some(unsigned), Seq(ByteString.copyFrom(proofs.arr))))
    }

    def broadcastLease(source: KeyPair, recipient: Recipient, amount: Long, fee: Long, version: Int = 2): Future[PBSignedTransaction] = {
      val unsigned = PBTransaction(
        chainId,
        ByteString.copyFrom(source.publicKey.arr),
        Some(Amount.of(ByteString.EMPTY, fee)),
        System.currentTimeMillis,
        version,
        PBTransaction.Data.Lease(LeaseTransactionData.of(Some(recipient), amount))
      )
      val proofs = crypto.sign(source.privateKey, PBTransactions.vanilla(SignedTransaction(Some(unsigned)), unsafe = true).explicitGet().bodyBytes())
      transactions.broadcast(SignedTransaction.of(Some(unsigned), Seq(ByteString.copyFrom(proofs.arr))))
    }

    def broadcastLeaseCancel(source: KeyPair, leaseId: String, fee: Long, version: Int = 2): Future[PBSignedTransaction] = {
      val unsigned = PBTransaction(
        chainId,
        ByteString.copyFrom(source.publicKey.arr),
        Some(Amount.of(ByteString.EMPTY, fee)),
        System.currentTimeMillis,
        version,
        PBTransaction.Data.LeaseCancel(LeaseCancelTransactionData.of(ByteString.copyFrom(Base58.decode(leaseId))))
      )
      val proofs = crypto.sign(source.privateKey, PBTransactions.vanilla(SignedTransaction(Some(unsigned)), unsafe = true).explicitGet().bodyBytes())
      transactions.broadcast(SignedTransaction.of(Some(unsigned), Seq(ByteString.copyFrom(proofs.arr))))
    }

    def setAssetScript(
        sender: KeyPair,
        assetId: String,
        script: Either[Array[Byte], Option[Scr]],
        fee: Long,
        timestamp: Long = System.currentTimeMillis(),
        version: Int = 1
    ): Future[PBSignedTransaction] = {
      val unsigned = PBTransaction(
        chainId,
        ByteString.copyFrom(sender.publicKey.arr),
        Some(Amount.of(ByteString.EMPTY, fee)),
        timestamp,
        version,
        PBTransaction.Data.SetAssetScript(SetAssetScriptTransactionData.of(ByteString.copyFrom(Base58.decode(assetId)), toPBScript(script)))
      )

      script match {
        case Left(_) => transactions.broadcast(SignedTransaction.of(Some(unsigned), Seq(ByteString.EMPTY)))
        case _ =>
          val proofs =
            crypto.sign(sender.privateKey, PBTransactions.vanilla(SignedTransaction(Some(unsigned)), unsafe = true).explicitGet().bodyBytes())
          transactions.broadcast(SignedTransaction.of(Some(unsigned), Seq(ByteString.copyFrom(proofs.arr))))
      }
    }

    def updateAssetInfo(
        sender: KeyPair,
        assetId: String,
        updatedName: String,
        updatedDescription: String,
        fee: Long,
        feeAsset: Asset = unitoken,
        version: TxVersion = TxVersion.V1
    ): Future[SignedTransaction] = {
      val unsigned = PBTransaction(
        chainId,
        ByteString.copyFrom(sender.publicKey.arr),
        Some(Amount.of(if (feeAsset == unitoken) ByteString.EMPTY else ByteString.copyFrom(Base58.decode(feeAsset.maybeBase58Repr.get)), fee)),
        System.currentTimeMillis(),
        version,
        PBTransaction.Data.UpdateAssetInfo(
          UpdateAssetInfoTransactionData.of(
            ByteString.copyFrom(Base58.decode(assetId)),
            updatedName,
            updatedDescription
          )
        )
      )

      val proofs      = crypto.sign(sender.privateKey, PBTransactions.vanilla(SignedTransaction(Some(unsigned)), unsafe = true).explicitGet().bodyBytes())
      val transaction = SignedTransaction.of(Some(unsigned), Seq(ByteString.copyFrom(proofs.arr)))

      transactions.broadcast(transaction)
    }

    def assetInfo(assetId: String): Future[AssetInfoResponse] = assets.getInfo(AssetRequest(ByteString.copyFrom(Base58.decode(assetId))))

    def getStatuses(request: TransactionsByIdRequest): Future[Seq[PBTransactionStatus]] = {
      val (obs, result) = createCallObserver[PBTransactionStatus]
      transactions.getStatuses(request, obs)
      result.runToFuture
    }
  }

  private def createCallObserver[T](implicit s: Scheduler): (StreamObserver[T], Task[List[T]]) = {
    val subj = ConcurrentSubject.replay[T]

    val observer = new StreamObserver[T] {
      override def onNext(value: T): Unit      = subj.onNext(value)
      override def onError(t: Throwable): Unit = subj.onError(t)
      override def onCompleted(): Unit         = subj.onComplete()
    }

    (observer, subj.toListL)
  }
}
