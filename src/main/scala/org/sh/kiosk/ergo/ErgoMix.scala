package org.sh.kiosk.ergo

import org.sh.cryptonode.util.BytesUtil._
import scorex.crypto.hash.Blake2b256
import sigmastate.basics.SecP256K1
import sigmastate.eval.SigmaDsl
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer
import special.sigma.GroupElement

object V1 {
  // in v1, the value gX is hardwired in script, while in v2, it is stored in registers
  val fullMixScriptSource =
    """{
      |  val c1 = SELF.R4[GroupElement].get
      |  val c2 = SELF.R5[GroupElement].get
      |  proveDlog(c2) ||            // either c2 is g^y
      |  proveDHTuple(g, c1, gX, c2) // or c2 is u^y = g^xy
      |}""".stripMargin

  val halfMixScriptSource =
    """{
      |  val c1 = OUTPUTS(0).R4[GroupElement].get
      |  val c2 = OUTPUTS(0).R5[GroupElement].get
      |
      |  OUTPUTS.size == 2 &&
      |  OUTPUTS(0).value == SELF.value &&
      |  OUTPUTS(1).value == SELF.value &&
      |  blake2b256(OUTPUTS(0).propositionBytes) == fullMixScriptHash &&
      |  blake2b256(OUTPUTS(1).propositionBytes) == fullMixScriptHash &&
      |  OUTPUTS(1).R4[GroupElement].get == c2 &&
      |  OUTPUTS(1).R5[GroupElement].get == c1 && {
      |    proveDHTuple(g, gX, c1, c2) ||
      |    proveDHTuple(g, gX, c2, c1)
      |  }
      |}""".stripMargin
}

object V2 {
  // in v2, the value gX is stored in registers and not hardwired
  val fullMixScriptSource =
    """{
      |  val u = SELF.R4[GroupElement].get // copied from previous transaction
      |  val c = SELF.R5[GroupElement].get
      |  val d = SELF.R6[GroupElement].get
      |  proveDlog(d) || proveDHTuple(g, c, u, d)
      |}""".stripMargin

  val halfMixScriptSource =
    """{
      |  val u = SELF.R4[GroupElement].get
      |  val u0 = OUTPUTS(0).R4[GroupElement].get
      |  val c0 = OUTPUTS(0).R5[GroupElement].get // w0
      |  val d0 = OUTPUTS(0).R6[GroupElement].get // w1
      |  val u1 = OUTPUTS(1).R4[GroupElement].get
      |  val c1 = OUTPUTS(1).R5[GroupElement].get // w1
      |  val d1 = OUTPUTS(1).R6[GroupElement].get // w0
      |  val bob = u0 == u && u1 == u && c0 == d1 && c1 == d0 &&
      |            (proveDHTuple(g, u, c0, d0) || proveDHTuple(g, u, d0, c0))
      |  val alice = proveDlog(u) // so Alice can spend if no one joins for a long time
      |  val fullMixBox = {(b:Box) => blake2b256(b.propositionBytes) == fullMixScriptHash}
      |  val fullMixTx = OUTPUTS(0).value == SELF.value && OUTPUTS(1).value == SELF.value &&
      |  fullMixBox(OUTPUTS(0)) && fullMixBox(OUTPUTS(1))
      |  fullMixTx && (bob || alice)
      |}""".stripMargin
}
object ErgoMix {
  // taken from examples

  // any variable/method starting with $ will not appear in front-end.
  // so any variable to be hidden from front-end is prefixed with $

  val $g = SigmaDsl.GroupElement(SecP256K1.generator)

  def $x: scala.math.BigInt = BigInt(Blake2b256("correct horse battery staple".getBytes)) // secret
  val $gX = SigmaDsl.GroupElement(SecP256K1.exponentiate(SecP256K1.generator, $x.bigInteger).normalize())
  val $gX_encoded = $gX.getEncoded.toArray.encodeHex

  def getSourceV1 = $getSource(V1.halfMixScriptSource, V1.fullMixScriptSource)
  def getSourceV2 = $getSource(V2.halfMixScriptSource, V2.fullMixScriptSource)

  def getScriptsV1 = $getScripts(V1.halfMixScriptSource, V1.fullMixScriptSource, Map("g" -> $g, "gX" -> $gX))
  def getScriptsV2 = $getScripts(V2.halfMixScriptSource, V2.fullMixScriptSource, Map("g" -> $g))

  def getScriptsMatchedV1 = $getScriptsMatched(V1.halfMixScriptSource, V1.fullMixScriptSource, Map("g" -> $g, "gX" -> $gX))
  def getScriptsMarchedV2 = $getScriptsMatched(V2.halfMixScriptSource, V2.fullMixScriptSource, Map("g" -> $g))

  def $getSource(halfMixScriptSource:String, fullMixScriptSource:String) = {
    Array(
      "fullMixScript \n"+fullMixScriptSource,
      "halfMixScript \n"+halfMixScriptSource
    )
  }

  def $getRawScripts(halfMixScriptSource:String, fullMixScriptSource:String, env:Map[String, GroupElement]) = {
    ErgoScript.env_clear
    env.foreach{
      case (name, value) => ErgoScript.env_setGroupElement(name, value)
    }
    val fullMixScriptBytes = DefaultSerializer.serializeErgoTree(ErgoScript.$compile(fullMixScriptSource))
    val fullMixScriptHash = scorex.crypto.hash.Blake2b256(fullMixScriptBytes)
    ErgoScript.env_setCollByte("fullMixScriptHash", fullMixScriptHash)
    val halfMixScriptBytes = DefaultSerializer.serializeErgoTree(ErgoScript.$compile(halfMixScriptSource))
    (fullMixScriptBytes, halfMixScriptBytes)
  }

  def $getScripts(halfMixScriptSource:String, fullMixScriptSource:String, env:Map[String, GroupElement]) = {
    val (fullMixScriptBytes, halfMixScriptBytes) = $getRawScripts(halfMixScriptSource, fullMixScriptSource, env)
    Array(
      s"gX = ${$gX_encoded}",
      s"fullMixScript = ${fullMixScriptBytes.encodeHex}".grouped(120).mkString("\n"),
      s"halfMixScript = ${halfMixScriptBytes.encodeHex}".grouped(120).mkString("\n")
    )
  }

  def $matchScript(scriptBytes:Array[Byte], env:Map[String, GroupElement]) = {
    env.foldLeft(scriptBytes.encodeHex)(
      (currStr, y) => {
        val (keyword, value) = y
        val encodedValue = value.getEncoded.toArray.encodeHex
        val value_r = encodedValue.length / 2
        val value_l = encodedValue.length - value_r
        val kw_r = keyword.length / 2
        val kw_l = keyword.length - kw_r
        val replacement = " <" + ("-" * (value_r - kw_r - 2))+" " + keyword + " "+("-" * (value_l - kw_l - 3)) + "> "
        currStr.replace(encodedValue, replacement)
      }
    )
  }
  def $getScriptsMatched(halfMixScriptSource:String, fullMixScriptSource:String, env:Map[String, GroupElement]) = {
    val (fullMixScriptBytes, halfMixScriptBytes) = $getRawScripts(halfMixScriptSource, fullMixScriptSource, env)

    env.map{
      case (keyword, value) =>
        keyword + " = " + value.getEncoded.toArray.encodeHex
    }.toArray ++ Array(
      ("fullMixScript = "+$matchScript(fullMixScriptBytes, env)).grouped(120).mkString("\n"),
      ("halfMixScript = "+$matchScript(halfMixScriptBytes, env)).grouped(120).mkString("\n")
    )

  }
}
