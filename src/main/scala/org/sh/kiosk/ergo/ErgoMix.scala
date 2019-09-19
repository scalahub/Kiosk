package org.sh.kiosk.ergo

import org.ergoplatform.{ErgoAddressEncoder, Pay2SAddress, Pay2SHAddress}
import org.sh.cryptonode.util.BytesUtil._
import scorex.crypto.hash.Blake2b256
import sigmastate.basics.SecP256K1
import sigmastate.eval.{CompiletimeIRContext, SigmaDsl}
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer
import special.sigma.GroupElement

object ErgoMix {
  // any variable/method starting with $ will not appear in front-end.
  // so any variable to be hidden from front-end is prefixed with $

  val $fullMixScriptSource =
    """{
      |  val c1 = SELF.R4[GroupElement].get
      |  val c2 = SELF.R5[GroupElement].get
      |  proveDlog(c2) ||            // either c2 is g^y
      |  proveDHTuple(g, c1, gX, c2) // or c2 is u^y = g^xy
      |}""".stripMargin

  val $halfMixScriptSource =
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

  val $ergoScript = new ErgoScript {}
  // any variable/method starting with $ will not appear in front-end.
  // so any variable to be hidden from front-end is prefixed with $

  val $g = SigmaDsl.GroupElement(SecP256K1.generator)

  // private key
  def $x: scala.math.BigInt = BigInt(Blake2b256("correct horse battery staple".getBytes)) // secret

  // public key
  val $gX = SigmaDsl.GroupElement(SecP256K1.exponentiate(SecP256K1.generator, $x.bigInteger).normalize())

  // encoded public key
  val $gX_encoded = $gX.getEncoded.toArray.encodeHex

  // ergoscript source
  def getSource = {
    Array(
      "halfMixScript \n"+$halfMixScriptSource,
      "fullMixScript \n"+$fullMixScriptSource
    )

  }

  val $myEnv = Map("g" -> $g, "gX" -> $gX)

  // ergoscript binary
  def getScripts = {
    val (halfMixScriptBytes, fullMixScriptBytes) = $getScriptBytes($halfMixScriptSource, $fullMixScriptSource, $myEnv)
    Array(
      s"gX = ${$gX_encoded}",
      s"halfMixScript = ${halfMixScriptBytes.encodeHex}".grouped(120).mkString("\n"),
      s"fullMixScript = ${fullMixScriptBytes.encodeHex}".grouped(120).mkString("\n")
    )
  }

  // ergoscript binary matched with env
  def getScriptsMatched(useRegex:Boolean) = {
    val $useRegex$ = "false"
    val (halfMixScriptBytes, fullMixScriptBytes) = $getScriptBytes($halfMixScriptSource, $fullMixScriptSource, $myEnv)
    val f:(Array[Byte], Array[String]) => String = if (useRegex) $ergoScript.$regex else $ergoScript.$matchScript
    $ergoScript.$env.collect{
      case (keyword, value:GroupElement) =>
        keyword + " = " + value.getEncoded.toArray.encodeHex
    }.toArray ++ Array(
      ("halfMixScript = "+f(halfMixScriptBytes, $ergoScript.$env.keys.toArray)).grouped(120).mkString("\n"),
      ("fullMixScript = "+f(fullMixScriptBytes, $ergoScript.$env.keys.toArray)).grouped(120).mkString("\n")
    )
  }

  import $ergoScript.$ergoAddressEncoder

  def getHalfMixBoxAddresses = {
    $getHalfMixBoxAddresses($halfMixScriptSource, $fullMixScriptSource, Map("g" -> $g, "gX" -> $gX))
  }

  def $getHalfMixBoxAddresses(halfMixScriptSource:String, fullMixScriptSource:String, env:Map[String, GroupElement]) = {
    val (halfMixTree, _) = $getRawScripts(halfMixScriptSource, fullMixScriptSource, env)
    val p2s = Pay2SAddress(halfMixTree)
    val p2sh = Pay2SHAddress(halfMixTree)
    Array(
      "P2S: "+p2s,
      "P2SH: "+p2sh
    )
  }

  def $getRawScripts(halfMixScriptSource:String, fullMixScriptSource:String, env:Map[String, GroupElement]) = {
    $ergoScript.env_clear
    env.foreach{
      case (name, value) => $ergoScript.env_setGroupElement(name, value)
    }
    val fullMixTree = $ergoScript.$compile(fullMixScriptSource)
    val fullMixScriptBytes = DefaultSerializer.serializeErgoTree(fullMixTree)
    val fullMixScriptHash = scorex.crypto.hash.Blake2b256(fullMixScriptBytes)
    $ergoScript.env_setCollByte("fullMixScriptHash", fullMixScriptHash)
    val halfMixTree = $ergoScript.$compile(halfMixScriptSource)
    (halfMixTree, fullMixTree)
  }

  def $getScriptBytes(halfMixScriptSource:String, fullMixScriptSource:String, env:Map[String, GroupElement]) = {
    val (halfMixTree, fullMixTree) = $getRawScripts(halfMixScriptSource, fullMixScriptSource, env)
    (DefaultSerializer.serializeErgoTree(halfMixTree), DefaultSerializer.serializeErgoTree(fullMixTree))
  }
}
