package com.decentralchain.database

import com.decentralchain.block.Block
import com.decentralchain.common.state.ByteStr
import com.decentralchain.state.Diff

trait Storage {
  def append(diff: Diff, carryFee: Long, totalFee: Long, reward: Option[Long], hitSource: ByteStr, block: Block): Unit
  def lastBlock: Option[Block]
  def rollbackTo(targetBlockId: ByteStr): Either[String, Seq[(Block, ByteStr)]]
}
