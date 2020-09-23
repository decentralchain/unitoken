package com.decentralchain

import com.decentralchain.account.{Address, KeyPair}
import com.decentralchain.common.state.ByteStr
import com.decentralchain.common.utils.EitherExt2
import com.decentralchain.lang.v1.estimator.ScriptEstimatorV1
import com.decentralchain.transaction.Asset.IssuedAsset
import com.decentralchain.transaction.TxHelpers
import com.decentralchain.transaction.smart.script.ScriptCompiler

object TestValues {
  val keyPair: KeyPair   = TxHelpers.defaultSigner
  val address: Address   = keyPair.toAddress
  val asset: IssuedAsset = IssuedAsset(ByteStr(("A" * 32).getBytes("ASCII")))
  val bigMoney: Long     = com.decentralchain.state.diffs.ENOUGH_AMT
  val timestamp: Long    = System.currentTimeMillis()
  val fee: Long          = 1e6.toLong

  val (script, scriptComplexity) = ScriptCompiler
    .compile(
      """
      |{-# STDLIB_VERSION 2 #-}
      |{-# CONTENT_TYPE EXPRESSION #-}
      |{-# SCRIPT_TYPE ACCOUNT #-}
      |true
      |""".stripMargin,
      ScriptEstimatorV1
    )
    .explicitGet()

  val (assetScript, assetScriptComplexity) = ScriptCompiler
    .compile(
      """
      |{-# STDLIB_VERSION 2 #-}
      |{-# CONTENT_TYPE EXPRESSION #-}
      |{-# SCRIPT_TYPE ASSET #-}
      |true
      |""".stripMargin,
      ScriptEstimatorV1
    )
    .explicitGet()

  val (rejectAssetScript, rejectAssetScriptComplexity) = ScriptCompiler
    .compile(
      """
      |{-# STDLIB_VERSION 2 #-}
      |{-# CONTENT_TYPE EXPRESSION #-}
      |{-# SCRIPT_TYPE ASSET #-}
      |false
      |""".stripMargin,
      ScriptEstimatorV1
    )
    .explicitGet()
}
