package com.decentralchain.lang.v1.traits.domain
import com.decentralchain.common.state.ByteStr

case class BlockInfo(timestamp: Long,
                     height: Int,
                     baseTarget: Long,
                     generationSignature: ByteStr,
                     generator: ByteStr,
                     generatorPublicKey: ByteStr,
                     vrf: Option[ByteStr])
