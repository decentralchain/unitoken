package com.decentralchain

import com.decentralchain.account.KeyPair
import com.decentralchain.block.{Block, MicroBlock}
import com.decentralchain.common.state.ByteStr
import com.decentralchain.common.utils.EitherExt2
import com.decentralchain.crypto._
import com.decentralchain.lagonaki.mocks.TestBlock
import com.decentralchain.transaction.Asset.unitoken
import com.decentralchain.transaction.transfer._
import monix.execution.schedulers.SchedulerService
import monix.execution.{Ack, Scheduler}
import monix.reactive.Observer
import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait RxScheduler extends BeforeAndAfterAll { _: Suite =>
  implicit val implicitScheduler: SchedulerService = Scheduler.singleThread("rx-scheduler")

  def testSchedulerName: String
  lazy val testScheduler: SchedulerService = Scheduler.singleThread(testSchedulerName)

  def test[A](f: => Future[A]): A = Await.result(f, 10.seconds)

  def send[A](p: Observer[A])(a: A): Future[Ack] =
    p.onNext(a)
      .map(ack => {
        Thread.sleep(500)
        ack
      })

  def byteStr(id: Int): ByteStr = ByteStr(Array.concat(Array.fill(SignatureLength - 1)(0), Array(id.toByte)))

  val signer: KeyPair = TestBlock.defaultSigner

  def block(id: Int): Block = TestBlock.create(Seq.empty).copy(signature = byteStr(id))

  def microBlock(total: Int, prev: Int): MicroBlock = {
    val tx = TransferTransaction.selfSigned(1.toByte, signer, signer.toAddress, unitoken, 1, unitoken, 1, ByteStr.empty, 1).explicitGet()
    MicroBlock.buildAndSign(3.toByte, signer, Seq(tx), byteStr(prev), byteStr(total)).explicitGet()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    implicitScheduler.shutdown()
    testScheduler.shutdown()
  }
}
