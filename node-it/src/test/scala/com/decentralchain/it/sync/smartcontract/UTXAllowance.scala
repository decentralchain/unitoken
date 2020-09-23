package com.decentralchain.it.sync.smartcontract

import com.typesafe.config.{Config, ConfigFactory}
import com.decentralchain.common.utils.EitherExt2
import com.decentralchain.it.api.SyncHttpApi._
import com.decentralchain.it.sync._
import com.decentralchain.it.transactions.NodesFromDocker
import com.decentralchain.it.util._
import com.decentralchain.it.{ReportingTestName, WaitForHeight2}
import com.decentralchain.lang.v1.estimator.v2.ScriptEstimatorV2
import com.decentralchain.transaction.smart.script.ScriptCompiler
import org.scalatest.{CancelAfterFailure, FreeSpec, Matchers}

class UTXAllowance extends FreeSpec with Matchers with WaitForHeight2 with CancelAfterFailure with ReportingTestName with NodesFromDocker {
  import UTXAllowance._

  override protected def nodeConfigs: Seq[Config] = Configs

  private def nodeA = nodes.head
  private def nodeB = nodes.last

  "create two nodes with scripted accounts and check UTX" in {
    val accounts = List(nodeA, nodeB).map(i => {

      val acc = i.createKeyPair()

      i.transfer(i.keyPair, acc.toAddress.toString, 10.unitoken, 0.005.unitoken, None, waitForTx = true)

      val scriptText = s"""true""".stripMargin
      val script     = ScriptCompiler(scriptText, isAssetScript = false, ScriptEstimatorV2).explicitGet()._1.bytes().base64
      i.setScript(acc, Some(script), setScriptFee, waitForTx = true)

      acc
    })

    assertBadRequestAndMessage(
      nodeA
        .transfer(
          accounts.head,
          recipient = accounts.head.toAddress.toString,
          assetId = None,
          amount = 1.unitoken,
          fee = minFee + 0.004.unitoken,
          version = 2
        ),
      "transactions from scripted accounts are denied from UTX pool"
    )

    val txBId =
      nodeB
        .transfer(
          accounts(1),
          recipient = accounts(1).toAddress.toString,
          assetId = None,
          amount = 1.01.unitoken,
          fee = minFee + 0.004.unitoken,
          version = 2
        )
        .id

    nodes.waitForHeightArise()
    nodeA.findTransactionInfo(txBId) shouldBe None
  }

}

object UTXAllowance {
  import com.decentralchain.it.NodeConfigs._
  private val FirstNode = ConfigFactory.parseString(s"""
                                                         |unitoken {
                                                         |  utx.allow-transactions-from-smart-accounts = false
                                                         |  miner {
                                                         |      quorum = 0
                                                         |      enable = yes
                                                         |  }
                                                         |}""".stripMargin)

  private val SecondNode = ConfigFactory.parseString(s"""
                                                          |unitoken {
                                                          |  utx.allow-transactions-from-smart-accounts = true
                                                          |  miner {
                                                          |      enable = no
                                                          |  }
                                                          |}""".stripMargin)

  val Configs: Seq[Config] = Seq(
    FirstNode.withFallback(Default.head),
    SecondNode.withFallback(Default(1))
  )

}
