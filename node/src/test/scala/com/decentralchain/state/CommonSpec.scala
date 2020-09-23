package com.decentralchain.state

import com.decentralchain.account.Address
import com.decentralchain.common.state.ByteStr
import com.decentralchain.common.utils.EitherExt2
import com.decentralchain.crypto.SignatureLength
import com.decentralchain.db.WithDomain
import com.decentralchain.lagonaki.mocks.TestBlock
import com.decentralchain.transaction.Asset.IssuedAsset
import com.decentralchain.transaction.GenesisTransaction
import com.decentralchain.{NoShrink, TestTime, TransactionGen}
import org.scalatest.{FreeSpec, Matchers}
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}

class CommonSpec extends FreeSpec with Matchers with WithDomain with TransactionGen with PropertyChecks with NoShrink {
  private val time          = new TestTime
  private def nextTs        = time.getTimestamp()
  private val AssetIdLength = 32

  private def genesisBlock(genesisTs: Long, address: Address, initialBalance: Long) = TestBlock.create(
    genesisTs,
    ByteStr(Array.fill[Byte](SignatureLength)(0)),
    Seq(GenesisTransaction.create(address, initialBalance, genesisTs).explicitGet())
  )

  "Common Conditions" - {
    "Zero balance of absent asset" in forAll(accountGen, positiveLongGen, byteArrayGen(AssetIdLength)) {
      case (sender, initialBalance, assetId) =>
        withDomain() { d =>
          d.appendBlock(genesisBlock(nextTs, sender.toAddress, initialBalance))
          d.balance(sender.toAddress, IssuedAsset(ByteStr(assetId))) shouldEqual 0L
        }
    }
  }
}
