package com.decentralchain.state.patch

import com.decentralchain.account.{Address, AddressScheme}
import com.decentralchain.common.utils.EitherExt2
import com.decentralchain.state.{Diff, _}

case object CancelInvalidLeaseIn extends DiffPatchFactory {
  val height: Int = AddressScheme.current.chainId.toChar match {
    case 'W' => 1060000
    case _   => 0
  }

  def apply(): Diff = {
    val pfs = PatchLoader.read[Map[String, LeaseBalance]](this).map {
      case (address, lb) =>
        Address.fromString(address).explicitGet() -> Portfolio(lease = lb)
    }
    Diff.empty.copy(portfolios = pfs)
  }
}
