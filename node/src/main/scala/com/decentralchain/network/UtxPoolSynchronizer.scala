package com.decentralchain.network

import cats.instances.int._
import com.google.common.cache.CacheBuilder
import com.decentralchain.common.state.ByteStr
import com.decentralchain.lang.ValidationError
import com.decentralchain.settings.SynchronizationSettings.UtxSynchronizerSettings
import com.decentralchain.transaction.TxValidationError.GenericError
import com.decentralchain.transaction.smart.script.trace.TracedResult
import com.decentralchain.transaction.{LastBlockInfo, Transaction}
import com.decentralchain.utils.{Schedulers, ScorexLogging}
import com.decentralchain.utx.UtxPool
import io.netty.channel.Channel
import io.netty.channel.group.ChannelGroup
import monix.eval.Task
import monix.execution.{AsyncQueue, Scheduler}
import monix.reactive.Observable

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionException, Future}
import scala.util.Success

trait UtxPoolSynchronizer {
  def tryPublish(tx: Transaction, source: Channel): Unit
  def publish(tx: Transaction): TracedResult[ValidationError, Boolean]
}

class UtxPoolSynchronizerImpl(
    val settings: UtxSynchronizerSettings,
    putIfNew: (Transaction, Boolean) => TracedResult[ValidationError, Boolean],
    broadcast: (Transaction, Option[Channel]) => Unit,
    blockSource: Observable[LastBlockInfo],
    timedScheduler: Scheduler
) extends UtxPoolSynchronizer
    with ScorexLogging
    with AutoCloseable {

  import Scheduler.Implicits.global

  private[this] val queue = AsyncQueue.bounded[(Transaction, Channel)](settings.maxQueueSize)

  private[this] val dummy = new Object()
  private[this] val knownTransactions = CacheBuilder
    .newBuilder()
    .maximumSize(settings.networkTxCacheSize)
    .build[ByteStr, Object]

  private[this] val pollLoopCancelable = Observable
    .repeatEvalF(Task.deferFuture(queue.poll()))
    .observeOn(Scheduler.global)
    .mapParallelUnordered(settings.maxThreads) {
      case (tx, source) =>
        Task
          .deferFuture(validateFuture(tx, allowRebroadcast = false, Some(source)))
          .timeout(5 seconds)
          .onErrorRecover { case err => TracedResult.wrapE(Left(GenericError(err.toString))) }
    }
    .doOnComplete(Task(log.info("UtxPoolSynchronizer stopped")))
    .doOnError(e => Task(log.warn("UtxPoolSynchronizer stopped abnormally", e)))
    .subscribe()

  blockSource.map(_.height).distinctUntilChanged.foreach(_ => knownTransactions.invalidateAll())

  override def tryPublish(tx: Transaction, source: Channel): Unit = {
    def transactionIsNew(txId: ByteStr): Boolean = {
      var isNew = false
      knownTransactions.get(txId, { () =>
        isNew = true; dummy
      })
      isNew
    }

    if (transactionIsNew(tx.id())) queue.tryOffer(tx -> source)
  }

  private[this] def validateFuture(
      tx: Transaction,
      allowRebroadcast: Boolean,
      source: Option[Channel]
  ): Future[TracedResult[ValidationError, Boolean]] =
    Schedulers
      .executeCatchingInterruptedException(timedScheduler)(putIfNew(tx, source.isEmpty))
      .recover {
        case err: ExecutionException if err.getCause.isInstanceOf[InterruptedException]  =>
          log.trace(s"Transaction took too long to validate: ${tx.id()}")
          TracedResult(Left(GenericError("Transaction took too long to validate")))

        case err =>
          log.warn(s"Error validating transaction ${tx.id()}", err)
          TracedResult(Left(GenericError(err)))
      }
      .andThen {
        case Success(TracedResult(Right(isNew), _)) if isNew || allowRebroadcast => broadcast(tx, source)
      }

  override def publish(tx: Transaction): TracedResult[ValidationError, Boolean] =
    Await.result(validateFuture(tx, settings.allowTxRebroadcasting, None), Duration.Inf)

  override def close(): Unit = pollLoopCancelable.cancel()
}

object UtxPoolSynchronizer extends ScorexLogging {
  def apply(
      utx: UtxPool,
      settings: UtxSynchronizerSettings,
      allChannels: ChannelGroup,
      blockSource: Observable[LastBlockInfo],
      sc: Scheduler
  ): UtxPoolSynchronizer =
    new UtxPoolSynchronizerImpl(
      settings,
      (tx, forceValidate) => utx.putIfNew(tx, forceValidate),
      (tx, ch) => allChannels.broadcast(tx, ch),
      blockSource,
      sc
    )

}
