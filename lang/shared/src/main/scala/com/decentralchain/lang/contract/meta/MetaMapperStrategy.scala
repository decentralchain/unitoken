package com.decentralchain.lang.contract.meta

import com.decentralchain.lang.v1.compiler.Types.FINAL
import com.decentralchain.protobuf.dapp.DAppMeta

private[meta] trait MetaMapperStrategy[V <: MetaVersion] {
  def toProto(data: List[List[FINAL]]): Either[String, DAppMeta]
  def fromProto(meta: DAppMeta): Either[String, List[List[FINAL]]]
}
