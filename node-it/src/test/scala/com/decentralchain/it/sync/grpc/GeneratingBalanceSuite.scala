package com.decentralchain.it.sync.grpc

import com.google.protobuf.ByteString
import com.decentralchain.account.KeyPair
import com.decentralchain.it.api.SyncGrpcApi._
import com.decentralchain.it.sync.minFee
import com.decentralchain.protobuf.transaction.Recipient

class GeneratingBalanceSuite extends GrpcBaseTransactionSuite {

  test("Generating balance should be correct") {
    val amount = 1000000000L

    val senderAddress = ByteString.copyFrom(sender.keyPair.toAddress.bytes)

    val recipient        = KeyPair("recipient".getBytes)
    val recipientAddress = ByteString.copyFrom(recipient.toAddress.bytes)

    val initialBalance = sender.unitokenBalance(senderAddress)

    sender.broadcastTransfer(sender.keyPair, Recipient().withPublicKeyHash(recipientAddress), amount, minFee, 2, waitForTx = true)

    val afterTransferBalance = sender.unitokenBalance(senderAddress)

    sender.broadcastTransfer(recipient, Recipient().withPublicKeyHash(senderAddress), amount - minFee, minFee, 2, waitForTx = true)

    val finalBalance = sender.unitokenBalance(senderAddress)

    assert(initialBalance.generating <= initialBalance.effective, "initial incorrect")
    assert(afterTransferBalance.generating <= afterTransferBalance.effective, "after transfer incorrect")
    assert(finalBalance.generating <= finalBalance.effective, "final incorrect")
  }
}
