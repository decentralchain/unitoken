package com.decentralchain.history

import com.decentralchain.db.WithDomain
import com.decentralchain.settings.unitokenSettings
import org.scalacheck.Gen
import org.scalatest.Suite
import org.scalatestplus.scalacheck.{ScalaCheckDrivenPropertyChecks => GeneratorDrivenPropertyChecks}

trait DomainScenarioDrivenPropertyCheck extends WithDomain { _: Suite with GeneratorDrivenPropertyChecks =>
  def scenario[S](gen: Gen[S], bs: unitokenSettings = DefaultunitokenSettings)(assertion: (Domain, S) => Any): Any =
    forAll(gen) { s =>
      withDomain(bs) { domain =>
        assertion(domain, s)
      }
    }
}
