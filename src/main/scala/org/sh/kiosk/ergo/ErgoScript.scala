package org.sh.kiosk.ergo

import org.ergoplatform.ErgoAddressEncoder.TestnetNetworkPrefix
import org.sh.easyweb.Text
import org.sh.cryptonode.ecc.{ECCPubKey, Point}
import org.sh.cryptonode.util.BytesUtil._
import org.sh.reflect.DefaultTypeHandler
import sigmastate.basics.SecP256K1
import sigmastate.eval.{CompiletimeIRContext, SigmaDsl}
import sigmastate.interpreter.Interpreter.ScriptNameProp
import sigmastate.lang.SigmaCompiler
import sigmastate.serialization.ErgoTreeSerializer
import special.sigma.GroupElement

case class ECPoint(point:Point) { // follows secp256k1 as in Bitcoin
  def toSigma = SigmaDsl.GroupElement(SecP256K1.createPoint(point.x.bigInteger, point.y.bigInteger))
}

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
  var $env:Map[String, Any] = Map()
  def $networkPrefix = if (ErgoAPI.$isMainNet) TestnetNetworkPrefix else TestnetNetworkPrefix
  def $compiler = SigmaCompiler($networkPrefix)
  implicit val $irContext = new CompiletimeIRContext
  def env_setGroupElement(name:String, groupElement: GroupElement) = {
    val $info$ = "A group element is encoded as a public key of Bitcoin in hex (compressed or uncompressed)"
    $env += name -> groupElement
    groupElement
  }
  //  def env_setGroupElement(name:String, pubKey:Array[Byte]) = {
  //    val $info$ = "A group element is encoded as a public key of Bitcoin in hex (compressed or uncompressed)"
  //    val point = ECCPubKey(pubKey.encodeHex).point
  //    val secp256k1Point = SecP256K1.createPoint(point.x.bigInteger, point.y.bigInteger)
  //    $env += name -> SigmaDsl.GroupElement(secp256k1Point)
  //  }
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
  def compile(ergoScript:Text) = {
    import sigmastate.lang.Terms._
    val tree = $compiler.compile($env, ergoScript.getText).asSigmaProp
    ErgoTreeSerializer.DefaultSerializer.serializeErgoTree(tree)
  }
}
