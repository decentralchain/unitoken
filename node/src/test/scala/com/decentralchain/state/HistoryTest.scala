package com.decentralchain.state

import com.decentralchain.block.Block
import com.decentralchain.common.state.ByteStr
import com.decentralchain.crypto._
import com.decentralchain.lagonaki.mocks.TestBlock

trait HistoryTest {
  val genesisBlock: Block = TestBlock.withReference(ByteStr(Array.fill(SignatureLength)(0: Byte)))

  def getNextTestBlock(blockchain: Blockchain): Block =
    TestBlock.withReference(blockchain.lastBlockId.get)

  def getNextTestBlockWithVotes(blockchain: Blockchain, votes: Seq[Short]): Block =
    TestBlock.withReferenceAndFeatures(blockchain.lastBlockId.get, votes)
}
