package com.decentralchain.state.patch

import com.decentralchain.state.{Blockchain, Diff}

trait DiffPatchFactory {
  def isApplicable(b: Blockchain): Boolean = b.height == this.height
  def height: Int
  def apply(): Diff
}
