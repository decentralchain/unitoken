package com.decentralchain.state

import com.decentralchain.block.Block.BlockId
import com.decentralchain.block.MicroBlock
import com.decentralchain.common.state.ByteStr

trait NG {
  def microBlock(id: ByteStr): Option[MicroBlock]

  def bestLastBlockInfo(maxTimestamp: Long): Option[BlockMinerInfo]

  def microblockIds: Seq[BlockId]
}
