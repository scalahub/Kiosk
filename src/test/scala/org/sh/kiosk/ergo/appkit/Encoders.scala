package org.sh.kiosk.ergo.appkit

import org.ergoplatform.appkit._
import org.ergoplatform.{ErgoAddressEncoder, Pay2SAddress}
import org.sh.cryptonode.util.BytesUtil._
import org.sh.cryptonode.util.StringUtil._
import org.sh.kiosk.ergo.encoding.ScalaErgoConverters
import org.sh.kiosk.ergo.script.{ECC, ErgoScript, ErgoScriptEnv}
import sigmastate.Values
import sigmastate.eval._
import sigmastate.interpreter.CryptoConstants
import special.sigma.GroupElement

object Encoders extends App {
  Client.usingClient { implicit ctx =>
    val fullMixScript = """{
                          |  val g = groupGenerator
                          |  val c1 = SELF.R4[GroupElement].get
                          |  val c2 = SELF.R5[GroupElement].get
                          |  val gX = SELF.R6[GroupElement].get
                          |  proveDlog(c2) ||          // either c2 is g^y
                          |  proveDHTuple(g, c1, gX, c2) // or c1 is g^y and c2 = gX^y = g^xy
                          |}""".stripMargin

    // first compute using Kiosk
    val ergoScript = new ErgoScript(new ErgoScriptEnv)
    val kioskScript = ergoScript.$compile(fullMixScript).bytes.encodeHex

    // then compute using appkit
    val appkitScript = ctx.compileContract(
      ConstantsBuilder.empty(), fullMixScript
    ).getErgoTree.bytes.encodeHex

    assert(appkitScript == kioskScript)
    println(s"Test passed: both scripts evaluated to ${appkitScript}")
  }

  Client.usingClient { implicit ctx =>
    val fullMixScript = """{
                          |  val g = groupGenerator
                          |  val c1 = SELF.R4[GroupElement].get
                          |  val c2 = SELF.R5[GroupElement].get
                          |  proveDlog(c2) ||          // either c2 is g^y
                          |  proveDHTuple(g, c1, gX, c2) // or c1 is g^y and c2 = gX^y = g^xy
                          |}""".stripMargin

    val x = BigInt("1120347812374928374923042340293450928435285028435028435")

    // first compute using Kiosk
    val env: ErgoScriptEnv = new ErgoScriptEnv
    val gX: String = ECC.gX(x)
    env.setGroupElement("gX", ScalaErgoConverters.stringToGroupElement(gX))
    val ergoScript: ErgoScript = new ErgoScript(env)
    val kioskScript: String = ergoScript.$compile(fullMixScript).bytes.encodeHex

    // then compute using appkit
    val g: GroupElement = CryptoConstants.dlogGroup.generator
    val gXappkit:GroupElement = g.exp(x.bigInteger)
    val appkitScript: String = ctx.compileContract(
      ConstantsBuilder.create().item(
        "gX", gXappkit
      ).build(), fullMixScript
    ).getErgoTree.bytes.encodeHex
    assert(appkitScript == kioskScript)
    println(s"Test passed: both scripts evaluated to ${appkitScript}")
  }

  Client.usingClient { implicit ctx =>
    val fullMixScript = """{
                          |  (blake2b256(OUTPUTS(0).propositionBytes) == hash) && getVar[Int](0).get == int &&
                          |  getVar[Long](1).get == long && getVar[BigInt](2).get == bigInt && SELF.R4[GroupElement].get == gX
                          |}""".stripMargin

    val x = BigInt("1120347812374928374923042340293450928435285028435028435")
    val hash: Array[Byte] = "1000d801d601e4c6a70507eb02cd7201cedb6a01dde4c6a70407e4c6a706077201".decodeHex
    val int = 238528959
    val long = 209384592083L
    val bigInt = BigInt("230948092384598209582958205802850298529085")

    // first compute using Kiosk
    val env: ErgoScriptEnv = new ErgoScriptEnv
    val gX: String = ECC.gX(x)
    env.setGroupElement("gX", ScalaErgoConverters.stringToGroupElement(gX))
    env.setCollByte("hash", hash)
    env.setBigInt("bigInt", bigInt)
    env.setLong("long", long)
    env.setInt("int", int)
    val ergoScript: ErgoScript = new ErgoScript(env)
    val kioskScript: String = ergoScript.$compile(fullMixScript).bytes.encodeHex

    // then compute using appkit
    val g: GroupElement = CryptoConstants.dlogGroup.generator
    val gXappkit:GroupElement = g.exp(x.bigInteger)
    val appkitScript: String = ctx.compileContract(
      ConstantsBuilder.create().item(
        "hash", hash
      ).item(
        "int", int
      ).item(
        "long", long
      ).item(
        "bigInt", SigmaDsl.BigInt(bigInt.bigInteger)
      ).item(
        "gX", gXappkit
      ).build(), fullMixScript
    ).getErgoTree.bytes.encodeHex
    assert(appkitScript == kioskScript)
    println(s"Test passed: both scripts evaluated to ${appkitScript}")
  }

  Client.usingClient { implicit ctx =>
    val fullMixScript = """{
                          |  sigmaProp(blake2b256(OUTPUTS(0).propositionBytes) == hash)
                          |}""".stripMargin

    val hash: Array[Byte] = "1000d801d601e4c6a70507eb02cd7201cedb6a01dde4c6a70407e4c6a706077201".decodeHex

    implicit val addressEncoder = new ErgoAddressEncoder(ctx.getNetworkType.networkPrefix)
    // first compute using Kiosk
    val env: ErgoScriptEnv = new ErgoScriptEnv
    env.setCollByte("hash", hash)
    val ergoScript: ErgoScript = new ErgoScript(env)
    val kioskScript: Values.ErgoTree = ergoScript.$compile(fullMixScript)
    val kioskAddress = Pay2SAddress(kioskScript).toString

    // then compute using appkit
    val appkitScript: Values.ErgoTree = ctx.compileContract(
      ConstantsBuilder.create().item(
        "hash", hash
      ).build(), fullMixScript
    ).getErgoTree

    val appkitAddress = addressEncoder.fromProposition(appkitScript).get.toString
    assert(kioskAddress == appkitAddress)
    println(s"Test passed: both scripts evaluated to ${appkitAddress}")
  }

}
