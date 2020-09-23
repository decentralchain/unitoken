package com.decentralchain.consensus.nxt.api.http

import akka.http.scaladsl.server.Route
import com.decentralchain.api.http.ApiError.BlockDoesNotExist
import com.decentralchain.api.http._
import com.decentralchain.block.BlockHeader
import com.decentralchain.common.state.ByteStr
import com.decentralchain.features.BlockchainFeatures
import com.decentralchain.settings.RestAPISettings
import com.decentralchain.state.Blockchain
import play.api.libs.json.{JsObject, Json}


case class NxtConsensusApiRoute(settings: RestAPISettings, blockchain: Blockchain) extends ApiRoute {

  override val route: Route =
    pathPrefix("consensus") {
      algo ~ basetarget ~ baseTargetId ~ generatingBalance
    }

  def generatingBalance: Route = (path("generatingbalance" / AddrSegment) & get) { address =>
    complete(Json.obj("address" -> address.stringRepr, "balance" -> blockchain.generatingBalance(address)))
  }

  private def headerForId(blockId: ByteStr, f: BlockHeader => JsObject) =
    complete {
      (for {
        height <- blockchain.heightOf(blockId)
        meta   <- blockchain.blockHeader(height)
      } yield f(meta.header)).toRight[ApiError](BlockDoesNotExist)
    }

  def baseTargetId: Route = (path("basetarget" / Signature) & get) { signature =>
    headerForId(signature, m => Json.obj("baseTarget" -> m.baseTarget))
  }

  def basetarget: Route = (path("basetarget") & get) {
    complete(
      blockchain.lastBlockHeader
        .map(m => Json.obj("baseTarget" -> m.header.baseTarget))
        .toRight(BlockDoesNotExist)
    )
  }

  def algo: Route = (path("algo") & get) {
    complete(
      if (blockchain.activatedFeatures.contains(BlockchainFeatures.FairPoS.id))
        Json.obj("consensusAlgo" -> "Fair Proof-of-Stake (FairPoS)")
      else
        Json.obj("consensusAlgo" -> "proof-of-stake (PoS)")
    )
  }
}
