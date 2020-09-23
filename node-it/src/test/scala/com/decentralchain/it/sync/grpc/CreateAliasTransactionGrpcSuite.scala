package com.decentralchain.it.sync.grpc

import com.decentralchain.account.AddressScheme
import com.decentralchain.common.utils.EitherExt2
import com.decentralchain.it.NTPTime
import com.decentralchain.it.api.SyncGrpcApi._
import com.decentralchain.it.sync.{aliasTxSupportedVersions, minFee, transferAmount}
import com.decentralchain.it.util._
import com.decentralchain.protobuf.transaction.{PBRecipients, Recipient}
import io.grpc.Status.Code
import org.scalatest.prop.TableDrivenPropertyChecks

import scala.util.Random

class CreateAliasTransactionGrpcSuite extends GrpcBaseTransactionSuite with NTPTime with TableDrivenPropertyChecks {

  val (aliasCreator, aliasCreatorAddr) = (firstAcc, firstAddress)
  test("Able to send money to an alias") {
    for (v <- aliasTxSupportedVersions) {
      val alias             = randomAlias()
      val creatorBalance    = sender.unitokenBalance(aliasCreatorAddr).available
      val creatorEffBalance = sender.unitokenBalance(aliasCreatorAddr).effective

      sender.broadcastCreateAlias(aliasCreator, alias, minFee, version = v, waitForTx = true)

      sender.unitokenBalance(aliasCreatorAddr).available shouldBe creatorBalance - minFee
      sender.unitokenBalance(aliasCreatorAddr).effective shouldBe creatorEffBalance - minFee

      sender.resolveAlias(alias) shouldBe PBRecipients.toAddress(aliasCreatorAddr.toByteArray, AddressScheme.current.chainId).explicitGet()

      sender.broadcastTransfer(aliasCreator, Recipient().withAlias(alias), transferAmount, minFee, waitForTx = true)

      sender.unitokenBalance(aliasCreatorAddr).available shouldBe creatorBalance - 2 * minFee
      sender.unitokenBalance(aliasCreatorAddr).effective shouldBe creatorEffBalance - 2 * minFee
    }
  }

  test("Not able to create same aliases to same address") {
    for (v <- aliasTxSupportedVersions) {
      val alias             = randomAlias()
      val creatorBalance    = sender.unitokenBalance(aliasCreatorAddr).available
      val creatorEffBalance = sender.unitokenBalance(aliasCreatorAddr).effective

      sender.broadcastCreateAlias(aliasCreator, alias, minFee, version = v, waitForTx = true)
      sender.unitokenBalance(aliasCreatorAddr).available shouldBe creatorBalance - minFee
      sender.unitokenBalance(aliasCreatorAddr).effective shouldBe creatorEffBalance - minFee

      assertGrpcError(sender.broadcastCreateAlias(aliasCreator, alias, minFee, version = v), "Alias already claimed", Code.INVALID_ARGUMENT)

      sender.unitokenBalance(aliasCreatorAddr).available shouldBe creatorBalance - minFee
      sender.unitokenBalance(aliasCreatorAddr).effective shouldBe creatorEffBalance - minFee
    }
  }

  test("Not able to create aliases to other addresses") {
    for (v <- aliasTxSupportedVersions) {
      val alias            = randomAlias()
      val secondBalance    = sender.unitokenBalance(secondAddress).available
      val secondEffBalance = sender.unitokenBalance(secondAddress).effective

      sender.broadcastCreateAlias(aliasCreator, alias, minFee, version = v, waitForTx = true)
      assertGrpcError(sender.broadcastCreateAlias(secondAcc, alias, minFee, version = v), "Alias already claimed", Code.INVALID_ARGUMENT)

      sender.unitokenBalance(secondAddress).available shouldBe secondBalance
      sender.unitokenBalance(secondAddress).effective shouldBe secondEffBalance
    }
  }

  val aliases_names =
    Table(s"aliasName${randomAlias()}", s"aaaa${randomAlias()}", s"....${randomAlias()}", s"123456789.${randomAlias()}", s"@.@-@_@${randomAlias()}")

  aliases_names.foreach { alias =>
    test(s"create alias named $alias") {
      for (v <- aliasTxSupportedVersions) {
        sender.broadcastCreateAlias(aliasCreator, s"$alias$v", minFee, version = v, waitForTx = true)
        sender.resolveAlias(s"$alias$v") shouldBe PBRecipients
          .toAddress(aliasCreatorAddr.toByteArray, AddressScheme.current.chainId)
          .explicitGet()
      }
    }
  }

  val invalid_aliases_names =
    Table(
      ("aliasName", "message"),
      ("", "Alias '' length should be between 4 and 30"),
      ("abc", "Alias 'abc' length should be between 4 and 30"),
      ("morethen_thirtycharactersinline", "Alias 'morethen_thirtycharactersinline' length should be between 4 and 30"),
      ("~!|#$%^&*()_+=\";:/?><|\\][{}", "Alias should contain only following characters: -.0123456789@_abcdefghijklmnopqrstuvwxyz"),
      ("multilnetest\ntest", "Alias should contain only following characters: -.0123456789@_abcdefghijklmnopqrstuvwxyz"),
      ("UpperCaseAliase", "Alias should contain only following characters: -.0123456789@_abcdefghijklmnopqrstuvwxyz")
    )

  forAll(invalid_aliases_names) { (alias: String, message: String) =>
    test(s"Not able to create alias named $alias") {
      for (v <- aliasTxSupportedVersions) {
        assertGrpcError(sender.broadcastCreateAlias(aliasCreator, alias, minFee, version = v), message, Code.INVALID_ARGUMENT)
      }
    }
  }

  test("Able to lease by alias") {
    for (v <- aliasTxSupportedVersions) {
      val (leaser, leaserAddr) = (thirdAcc, thirdAddress)
      val alias                = randomAlias()

      val aliasCreatorBalance    = sender.unitokenBalance(aliasCreatorAddr).available
      val aliasCreatorEffBalance = sender.unitokenBalance(aliasCreatorAddr).effective
      val leaserBalance          = sender.unitokenBalance(leaserAddr).available
      val leaserEffBalance       = sender.unitokenBalance(leaserAddr).effective

      sender.broadcastCreateAlias(aliasCreator, alias, minFee, version = v, waitForTx = true)
      val leasingAmount = 1.unitoken

      sender.broadcastLease(leaser, Recipient().withAlias(alias), leasingAmount, minFee, waitForTx = true)

      sender.unitokenBalance(aliasCreatorAddr).available shouldBe aliasCreatorBalance - minFee
      sender.unitokenBalance(aliasCreatorAddr).effective shouldBe aliasCreatorEffBalance + leasingAmount - minFee
      sender.unitokenBalance(leaserAddr).available shouldBe leaserBalance - leasingAmount - minFee
      sender.unitokenBalance(leaserAddr).effective shouldBe leaserEffBalance - leasingAmount - minFee
    }
  }

  test("Not able to create alias when insufficient funds") {
    for (v <- aliasTxSupportedVersions) {
      val balance = sender.unitokenBalance(aliasCreatorAddr).available
      val alias   = randomAlias()
      assertGrpcError(
        sender.broadcastCreateAlias(aliasCreator, alias, balance + minFee, version = v),
        "Accounts balance errors",
        Code.INVALID_ARGUMENT
      )
    }
  }

  private def randomAlias(): String = {
    s"testalias.${Random.alphanumeric.take(9).mkString}".toLowerCase
  }

}
