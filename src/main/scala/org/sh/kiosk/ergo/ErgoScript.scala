package org.sh.kiosk.ergo

import org.ergoplatform.ErgoAddressEncoder.{MainnetNetworkPrefix, TestnetNetworkPrefix}
import org.ergoplatform.{ErgoAddressEncoder, Pay2SAddress, Pay2SHAddress}
import org.sh.cryptonode.ecc
import org.sh.cryptonode.ecc.{ECCPubKey, Point}
import org.sh.cryptonode.util.BytesUtil._
import org.sh.cryptonode.util.StringUtil._
import org.sh.easyweb.Text
import org.sh.reflect.DefaultTypeHandler
import sigmastate.Values.ErgoTree
import sigmastate.basics.SecP256K1
import sigmastate.eval.{CompiletimeIRContext, SigmaDsl}
import sigmastate.interpreter.Interpreter.ScriptNameProp
import sigmastate.lang.SigmaCompiler
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer
import special.sigma.GroupElement

object ErgoScript {

  DefaultTypeHandler.addType[GroupElement](
    classOf[GroupElement],
    hex => {
      val point = ECCPubKey(hex).point
      val secp256k1Point = SecP256K1.createPoint(point.x.bigInteger, point.y.bigInteger)
      SigmaDsl.GroupElement(secp256k1Point)
    },
    g => g.getEncoded.toArray.encodeHex
  )

  DefaultTypeHandler.addType[ErgoTree](
    classOf[ErgoTree],
    str => {
      val bytes = str.decodeHex
      DefaultSerializer.deserializeErgoTree(bytes)
    },
    DefaultSerializer.serializeErgoTree(_).encodeHex
  )

  var $env:Map[String, Any] = Map()
  def $networkPrefix = if (ErgoAPI.$isMainNet) MainnetNetworkPrefix else TestnetNetworkPrefix
  def $compiler = SigmaCompiler($networkPrefix)
  implicit val $irContext = new CompiletimeIRContext
  implicit val $ergoAddressEncoder: ErgoAddressEncoder = new ErgoAddressEncoder($networkPrefix)

  def env_setGroupElement(name:String, groupElement: GroupElement) = {
    val $INFO$ = "A group element is encoded as a public key of Bitcoin in hex (compressed or uncompressed)"
    $env += name -> groupElement
    groupElement
  }

  def env_clear = {
    $env = Map()
  }
  def env_setScriptName(scriptName:String) = {
    $env += ScriptNameProp -> scriptName
  }
  def env_setInt(name:String, int:Int) = {
    $env += name -> int
  }
  def env_setLong(name:String, long:Long  ) = {
    $env += name -> long
  }
  def env_setCollByte(name:String, collBytes:Array[Byte]) = {
    $env += name -> collBytes
  }
  def env_setCollCollByte(name:String, collCollBytes:Array[Array[Byte]]) = {
    $env += name -> collCollBytes
  }

  def $compile(ergoScript:String):ErgoTree = {
    import sigmastate.lang.Terms._
    $compiler.compile($env, ergoScript).asSigmaProp
  }
  def compile(ergoScript:Text):ErgoTree = {
    $compile(ergoScript.getText)
  }

  def getDefaultGenerator = {
    new ECCPubKey(org.sh.cryptonode.ecc.Util.G, true).hex
  }

  def getGroupElement(exponent:BigInt) = {
    val g = SecP256K1.generator
    val h = SecP256K1.exponentiate(g, exponent.bigInteger).normalize()
    val x = h.getXCoord.toBigInteger
    val y = h.getYCoord.toBigInteger
    ECCPubKey(Point(x, y), true).hex
  }

  def getP2SH_Address(ergoScript:Text) = {
    Pay2SHAddress(compile(ergoScript)).toString
  }

  def getP2S_Address(ergoScript:Text) = {
    Pay2SAddress(compile(ergoScript)).toString
  }
}
