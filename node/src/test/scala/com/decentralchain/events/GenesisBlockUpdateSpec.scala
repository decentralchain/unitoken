package com.decentralchain.events

import com.decentralchain.settings.unitokenSettings
import com.decentralchain.state.diffs.ENOUGH_AMT
import com.decentralchain.transaction.GenesisTransaction
import com.decentralchain.{BlockGen, TestHelpers}
import org.scalacheck.Gen
import org.scalatest.{FreeSpec, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import com.decentralchain.common.utils.EitherExt2

class GenesisBlockUpdateSpec extends FreeSpec with Matchers with BlockGen with ScalaCheckPropertyChecks with EventsHelpers {
  override protected def settings: unitokenSettings = TestHelpers.enableNG(super.settings)

  val genesisAppendWithunitokenAmountGen: Gen[(BlockAppended, Long)] = for {
    master      <- accountGen
    unitokenAmount <- Gen.choose(1L, ENOUGH_AMT)
    gt = GenesisTransaction.create(master.toAddress, unitokenAmount, 0).explicitGet()
    b <- blockGen(Seq(gt), master)
    ba = appendBlock(b)
  } yield (ba, unitokenAmount)

  "on genesis block append" - {
    "master address balance gets correctly updated" in forAll(genesisAppendWithunitokenAmountGen) {
      case (BlockAppended(_, _, _, _, _, upds), unitokenAmount) =>
        upds.head.balances.head._3 shouldBe unitokenAmount
    }

    "updated unitoken amount is calculated correctly" in forAll(genesisAppendWithunitokenAmountGen) {
      case (BlockAppended(_, _, _, updatedunitokenAmount, _, _), unitokenAmount) =>
        updatedunitokenAmount shouldBe unitokenAmount
    }
  }

}
