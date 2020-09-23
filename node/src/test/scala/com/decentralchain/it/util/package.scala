package com.decentralchain.it

import com.decentralchain.settings.Constants

package object util {
  implicit class DoubleExt(val d: Double) extends AnyVal {
    def unitoken: Long = (BigDecimal(d) * Constants.UnitsInWave).toLong
  }
}
