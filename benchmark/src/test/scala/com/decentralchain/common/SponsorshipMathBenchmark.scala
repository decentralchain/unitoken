package com.decentralchain.common
import java.util.concurrent.TimeUnit

import com.decentralchain.state.diffs.FeeValidation
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Array(Mode.Throughput))
@Threads(4)
@Fork(1)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
class SponsorshipMathBenchmark {
  @Benchmark
  def bigDecimal_test(bh: Blackhole): Unit = {
    def tounitoken(assetFee: Long, sponsorship: Long): Long = {
      val unitoken = (BigDecimal(assetFee) * BigDecimal(FeeValidation.FeeUnit)) / BigDecimal(sponsorship)
      if (unitoken > Long.MaxValue) {
        throw new java.lang.ArithmeticException("Overflow")
      }
      unitoken.toLong
    }

    bh.consume(tounitoken(100000, 100000000))
  }

  @Benchmark
  def bigInt_test(bh: Blackhole): Unit = {
    def tounitoken(assetFee: Long, sponsorship: Long): Long = {
      val unitoken = BigInt(assetFee) * FeeValidation.FeeUnit / sponsorship
      unitoken.bigInteger.longValueExact()
    }

    bh.consume(tounitoken(100000, 100000000))
  }
}
