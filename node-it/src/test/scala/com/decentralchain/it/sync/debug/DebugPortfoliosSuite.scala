package com.decentralchain.it.sync.debug

import com.typesafe.config.Config
import com.decentralchain.it.{Node, NodeConfigs}
import com.decentralchain.it.api.SyncHttpApi._
import com.decentralchain.it.transactions.NodesFromDocker
import com.decentralchain.it.util._
import com.decentralchain.it.sync._
import org.scalatest.FunSuite

class DebugPortfoliosSuite extends FunSuite with NodesFromDocker {
  override protected def nodeConfigs: Seq[Config] =
    NodeConfigs.newBuilder
      .overrideBase(_.quorum(0))
      .withDefault(entitiesNumber = 1)
      .buildNonConflicting()

  private def sender: Node = nodes.head

  private lazy val firstAcc  = sender.createKeyPair()
  private lazy val secondAcc = sender.createKeyPair()

  private lazy val firstAddress: String  = firstAcc.toAddress.toString
  private lazy val secondAddress: String = secondAcc.toAddress.toString

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    sender.transfer(sender.keyPair, firstAddress, 20.unitoken, minFee, waitForTx = true)
    sender.transfer(sender.keyPair, secondAddress, 20.unitoken, minFee, waitForTx = true)
  }

  test("getting a balance considering pessimistic transactions from UTX pool - changed after UTX") {
    val portfolioBefore = sender.debugPortfoliosFor(firstAddress, considerUnspent = true)
    val utxSizeBefore   = sender.utxSize

    sender.transfer(firstAcc, secondAddress, 5.unitoken, 5.unitoken)
    sender.transfer(secondAcc, firstAddress, 7.unitoken, 5.unitoken)

    sender.waitForUtxIncreased(utxSizeBefore)

    val portfolioAfter = sender.debugPortfoliosFor(firstAddress, considerUnspent = true)

    val expectedBalance = portfolioBefore.balance - 10.unitoken // withdraw + fee
    assert(portfolioAfter.balance == expectedBalance)

  }

  test("getting a balance without pessimistic transactions from UTX pool - not changed after UTX") {
    nodes.waitForHeightArise()

    val portfolioBefore = sender.debugPortfoliosFor(firstAddress, considerUnspent = false)
    val utxSizeBefore   = sender.utxSize

    sender.transfer(firstAcc, secondAddress, 5.unitoken, fee = 5.unitoken)
    sender.waitForUtxIncreased(utxSizeBefore)

    val portfolioAfter = sender.debugPortfoliosFor(firstAddress, considerUnspent = false)
    assert(portfolioAfter.balance == portfolioBefore.balance)
  }
}
