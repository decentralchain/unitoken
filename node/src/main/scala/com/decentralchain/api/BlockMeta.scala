package com.decentralchain.api

import com.decentralchain.block.Block.protoHeaderHash
import com.decentralchain.block.serialization.BlockHeaderSerializer
import com.decentralchain.block.{Block, BlockHeader, SignedBlockHeader}
import com.decentralchain.common.state.ByteStr
import monix.eval.Coeval
import play.api.libs.json.{JsObject, Json}

case class BlockMeta(
    header: BlockHeader,
    signature: ByteStr,
    headerHash: Option[ByteStr],
    height: Int,
    size: Int,
    transactionCount: Int,
    totalFeeInunitoken: Long,
    reward: Option[Long],
    vrf: Option[ByteStr]
) {
  def toSignedHeader: SignedBlockHeader = SignedBlockHeader(header, signature)
  def id: ByteStr                       = headerHash.getOrElse(signature)

  val json: Coeval[JsObject] = Coeval.evalOnce {
    BlockHeaderSerializer.toJson(header, size, transactionCount, signature) ++
      Json.obj("height" -> height, "totalFee" -> totalFeeInunitoken) ++
      reward.fold(Json.obj())(r => Json.obj("reward" -> r)) ++
      vrf.fold(Json.obj())(v => Json.obj("VRF"       -> v.toString)) ++
      headerHash.fold(Json.obj())(h => Json.obj("id" -> h.toString))
  }
}

object BlockMeta {
  def fromBlock(block: Block, height: Int, totalFee: Long, reward: Option[Long], vrf: Option[ByteStr]): BlockMeta = BlockMeta(
    block.header,
    block.signature,
    if (block.header.version >= Block.ProtoBlockVersion) Some(protoHeaderHash(block.header)) else None,
    height,
    block.bytes().length,
    block.transactionData.length,
    totalFee,
    reward,
    vrf
  )
}
