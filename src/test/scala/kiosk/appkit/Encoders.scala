package kiosk.appkit

import kiosk.ECC
import kiosk.encoding.ScalaErgoConverters
import kiosk.script.{ErgoScript, ErgoScriptEnv}
import org.ergoplatform.appkit._
import org.ergoplatform.{ErgoAddress, ErgoAddressEncoder, Pay2SAddress}
import org.sh.cryptonode.util.BytesUtil._
import org.sh.cryptonode.util.StringUtil._
import sigmastate.Values
import sigmastate.eval._
import sigmastate.interpreter.CryptoConstants
import special.sigma.GroupElement

object Encoders extends App {
  val tests: Seq[Unit] = Seq(test1, test2, test3, test4, test5, test6)

  def test1 = Client.usingClient { implicit ctx =>
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

  def test2 = Client.usingClient { implicit ctx =>
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

  def test3 = Client.usingClient { implicit ctx =>
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

  def test4 = Client.usingClient { implicit ctx =>
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
    println(s"Test passed: both addresses evaluated to ${appkitAddress}")
  }

  // below tests some address encoding techniques. Addresses have some quirks; for example fANwcUDKxKD3btGmknic2kE7mEzLR2CFTYzEKPh5iyPMUMwfwjuxsJP and 9hTh4u6CDXktMQb9BoRo5nTPnmFN8G5u4PUCURvoUCXmtaaDYdw contain the same ErgoTree
  def test5 = {
    import ErgoScript._
    val address: String = "9hTh4u6CDXktMQb9BoRo5nTPnmFN8G5u4PUCURvoUCXmtaaDYdw"
    val ergoAddress: ErgoAddress = ScalaErgoConverters.getAddressFromString(address)

    assert(ergoAddress.toString == address)

    val script: Values.ErgoTree = ergoAddress.script
    val scriptHex: String = script.bytes.encodeHex

    val p2sAddress: Pay2SAddress = Pay2SAddress(script)
    val p2sAddressScript: Values.ErgoTree = p2sAddress.script
    val p2sAddressScriptBytes: Array[Byte] = p2sAddress.scriptBytes

    assert(scriptHex == p2sAddressScript.bytes.encodeHex)
    assert(scriptHex == p2sAddressScriptBytes.encodeHex)

    val p2sAddressString: String = ScalaErgoConverters.getStringFromAddress(p2sAddress)
    val p2sAddressToErgoAddress: ErgoAddress = ScalaErgoConverters.getAddressFromString(p2sAddressString)
    val p2SAddressStringToScript: Values.ErgoTree = p2sAddressToErgoAddress.script

    assert(scriptHex == p2SAddressStringToScript.bytes.encodeHex)

    assert(p2sAddressString == "fANwcUDKxKD3btGmknic2kE7mEzLR2CFTYzEKPh5iyPMUMwfwjuxsJP") // ToDo: check why this address is encoded differently from the original

    assert(
      ScalaErgoConverters.getStringFromAddress(
        ScalaErgoConverters.getAddressFromString(p2sAddressString)
      ) == p2sAddressString
    )
  }
  def test6 = {
    import org.apache.commons.codec.binary.Hex
    import org.ergoplatform.ErgoAddressEncoder
    implicit val addressEncoder = new ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)
    val address1 = addressEncoder.fromString("fANwcUDKxKD3btGmknic2kE7mEzLR2CFTYzEKPh5iyPMUMwfwjuxsJP").get
    val address2 = addressEncoder.fromString("9hTh4u6CDXktMQb9BoRo5nTPnmFN8G5u4PUCURvoUCXmtaaDYdw").get
    val hex1 = Hex.encodeHexString(address1.script.bytes)
    val hex2 = Hex.encodeHexString(address2.script.bytes)
    assert(hex1 == hex2)
    assert(hex1 == "0008cd03836fd1f810cbfa6aa9516530709ae6e591bccb9523e9b65c49c09586319d10de")
    assert(Pay2SAddress(address2.script).toString() == "fANwcUDKxKD3btGmknic2kE7mEzLR2CFTYzEKPh5iyPMUMwfwjuxsJP")
    assert(addressEncoder.fromProposition(address1.script).get.toString == "9hTh4u6CDXktMQb9BoRo5nTPnmFN8G5u4PUCURvoUCXmtaaDYdw")

    val ergoTree = ScalaErgoConverters.getAddressFromString("9hTh4u6CDXktMQb9BoRo5nTPnmFN8G5u4PUCURvoUCXmtaaDYdw").script
    val address3 = Pay2SAddress(ergoTree).toString
    val address4 = ScalaErgoConverters.getAddressFromErgoTree(ergoTree).toString
    assert(address3 == "fANwcUDKxKD3btGmknic2kE7mEzLR2CFTYzEKPh5iyPMUMwfwjuxsJP")
    assert(address4 == "9hTh4u6CDXktMQb9BoRo5nTPnmFN8G5u4PUCURvoUCXmtaaDYdw")
    assert(addressEncoder.toString(ScalaErgoConverters.getAddressFromString("9hTh4u6CDXktMQb9BoRo5nTPnmFN8G5u4PUCURvoUCXmtaaDYdw")) == "9hTh4u6CDXktMQb9BoRo5nTPnmFN8G5u4PUCURvoUCXmtaaDYdw")
  }
}
