package com.decentralchain.protobuf

package object block {
  type PBBlock = com.decentralchain.protobuf.block.Block
  val PBBlock = com.decentralchain.protobuf.block.Block

  type VanillaBlock = com.decentralchain.block.Block
  val VanillaBlock = com.decentralchain.block.Block

  type PBBlockHeader = com.decentralchain.protobuf.block.Block.Header
  val PBBlockHeader = com.decentralchain.protobuf.block.Block.Header

  type VanillaBlockHeader = com.decentralchain.block.BlockHeader
  val VanillaBlockHeader = com.decentralchain.block.BlockHeader

  type PBSignedMicroBlock = com.decentralchain.protobuf.block.SignedMicroBlock
  val PBSignedMicroBlock = com.decentralchain.protobuf.block.SignedMicroBlock

  type PBMicroBlock = com.decentralchain.protobuf.block.MicroBlock
  val PBMicroBlock = com.decentralchain.protobuf.block.MicroBlock

  type VanillaMicroBlock = com.decentralchain.block.MicroBlock
  val VanillaMicroBlock = com.decentralchain.block.MicroBlock
}
