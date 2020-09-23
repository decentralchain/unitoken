package com.decentralchain.history

import com.decentralchain.common.utils.EitherExt2
import com.decentralchain.features.BlockchainFeatures
import com.decentralchain.history.Domain.BlockchainUpdaterExt
import com.decentralchain.state.diffs._
import com.decentralchain.transaction.GenesisTransaction
import com.decentralchain.transaction.transfer._
import com.decentralchain.{EitherMatchers, TransactionGen}
import org.scalacheck.Gen
import org.scalatest._
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}

class BlockchainUpdaterGeneratorFeeSameBlockTest
    extends PropSpec
    with PropertyChecks
    with DomainScenarioDrivenPropertyCheck
    with Matchers
    with EitherMatchers
    with TransactionGen {

  type Setup = (GenesisTransaction, TransferTransaction, TransferTransaction)

  val preconditionsAndPayments: Gen[Setup] = for {
    sender    <- accountGen
    recipient <- accountGen
    fee       <- smallFeeGen
    ts        <- positiveIntGen
    genesis: GenesisTransaction = GenesisTransaction.create(sender.toAddress, ENOUGH_AMT, ts).explicitGet()
    payment: TransferTransaction <- unitokenTransferGeneratorP(ts, sender, recipient.toAddress)
    generatorPaymentOnFee: TransferTransaction = createunitokenTransfer(defaultSigner, recipient.toAddress, payment.fee, fee, ts + 1).explicitGet()
  } yield (genesis, payment, generatorPaymentOnFee)

  property("block generator can spend fee after transaction before applyMinerFeeWithTransactionAfter") {
    assume(BlockchainFeatures.implemented.contains(BlockchainFeatures.SmartAccounts.id))
    scenario(preconditionsAndPayments, DefaultunitokenSettings) {
      case (domain, (genesis, somePayment, generatorPaymentOnFee)) =>
        val blocks = chainBlocks(Seq(Seq(genesis), Seq(generatorPaymentOnFee, somePayment)))
        blocks.foreach(block => domain.blockchainUpdater.processBlock(block) should beRight)
    }
  }

  property("block generator can't spend fee after transaction after applyMinerFeeWithTransactionAfter") {
    scenario(preconditionsAndPayments, MicroblocksActivatedAt0unitokenSettings) {
      case (domain, (genesis, somePayment, generatorPaymentOnFee)) =>
        val blocks = chainBlocks(Seq(Seq(genesis), Seq(generatorPaymentOnFee, somePayment)))
        blocks.init.foreach(block => domain.blockchainUpdater.processBlock(block) should beRight)
        domain.blockchainUpdater.processBlock(blocks.last) should produce("unavailable funds")
    }
  }
}
