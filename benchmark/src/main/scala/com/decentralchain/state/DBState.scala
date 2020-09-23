package com.decentralchain.state

import java.io.File

import com.decentralchain.Application
import com.decentralchain.account.AddressScheme
import com.decentralchain.common.state.ByteStr
import com.decentralchain.database.{LevelDBWriter, openDB}
import com.decentralchain.lang.directives.DirectiveSet
import com.decentralchain.settings.unitokenSettings
import com.decentralchain.transaction.smart.unitokenEnvironment
import com.decentralchain.utils.ScorexLogging
import monix.eval.Coeval
import org.iq80.leveldb.DB
import org.openjdk.jmh.annotations.{Param, Scope, State, TearDown}

@State(Scope.Benchmark)
abstract class DBState extends ScorexLogging {
  @Param(Array("unitoken.conf"))
  var configFile = ""

  lazy val settings: unitokenSettings = Application.loadApplicationConfig(Some(new File(configFile)).filter(_.exists()))

  lazy val db: DB = openDB(settings.dbSettings.directory)

  lazy val levelDBWriter: LevelDBWriter =
    LevelDBWriter.readOnly(
      db,
      settings.copy(dbSettings = settings.dbSettings.copy(maxCacheSize = 1))
    )

  AddressScheme.current = new AddressScheme { override val chainId: Byte = 'W' }

  lazy val environment = new unitokenEnvironment(
    AddressScheme.current.chainId,
    Coeval.raiseError(new NotImplementedError("`tx` is not implemented")),
    Coeval(levelDBWriter.height),
    levelDBWriter,
    null,
    DirectiveSet.contractDirectiveSet,
    ByteStr.empty
  )

  @TearDown
  def close(): Unit = {
    db.close()
  }
}
