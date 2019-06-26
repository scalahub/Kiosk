package org.sh.kiosk.ergo

import org.ergoplatform.ErgoAddressEncoder.{MainnetNetworkPrefix, TestnetNetworkPrefix}
import org.ergoplatform.{ErgoAddressEncoder, Pay2SAddress, Pay2SHAddress}
import org.sh.cryptonode.ecc.{ECCPubKey, Point}
import org.sh.cryptonode.util.BytesUtil._
import org.sh.cryptonode.util.StringUtil._
import org.sh.easyweb.Text
import org.sh.reflect.DefaultTypeHandler
import sigmastate.Values.ErgoTree
import sigmastate.basics.SecP256K1
import sigmastate.eval.{CGroupElement, CompiletimeIRContext, SigmaDsl}
import sigmastate.interpreter.Interpreter.ScriptNameProp
import sigmastate.lang.SigmaCompiler
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer
import special.collection.Coll
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

  def $arrByteToCollByte(a:Array[Byte]) = sigmastate.eval.Colls.fromArray(a)
  def $arrArrByteToCollByte(a:Array[Array[Byte]]) = {
    val collArray = a.map{colByte =>
      sigmastate.eval.Colls.fromArray(colByte)
    }
    sigmastate.eval.Colls.fromArray(collArray)
  }


  def env_get = {
    $env.toArray.map{
      case (name, value) =>
        val (elValue, elType) = value match {
          case grp: GroupElement => (grp.getEncoded.toArray.encodeHex, "GroupElement")
          case bigInt: BigInt => (bigInt.toString(10), "BigInt")
          // case bigInt: special.sigma.BigInt => (bigInt.toBytes.toArray.encodeHex, "BigInt")
          case collByte: Array[Byte] => (DefaultTypeHandler.typeToString(collByte.getClass, collByte), "Coll[Byte]")
          case collCollByte: Array[Array[Byte]] => (DefaultTypeHandler.typeToString(collCollByte.getClass, collCollByte), "Coll[Coll[Byte]]")
          case any => (any.toString, any.getClass)
        }
        s"""{"name":"$name", "value":"${elValue}", "type":"${elType}"}"""
    }
  }

  def $convertedEnv = {
    $env.map{
      case (key, value) =>
        val convertedValue = value match {
          case bigInt:BigInt => SigmaDsl.BigInt(bigInt.bigInteger)
          case collBytes:Array[Byte] => sigmastate.eval.Colls.fromArray(collBytes)
          case collCollBytes:Array[Array[Byte]] =>
            val collArray = collCollBytes.map{collBytes =>
              sigmastate.eval.Colls.fromArray(collBytes)
            }
            sigmastate.eval.Colls.fromArray(collArray)
          case grp:GroupElement => grp
          case any => any
        }
        key -> convertedValue
    }
  }

  def $compile(ergoScript:String):ErgoTree = {
    import sigmastate.lang.Terms._
    $compiler.compile($convertedEnv, ergoScript).asSigmaProp
  }

  def env_setGroupElement(name:String, groupElement: GroupElement) = {
    val $INFO$ = "A group element is encoded as a public key of Bitcoin in hex (compressed or uncompressed)"
    val $name$ = "g"
    val $groupElement$ = "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"
    $env += name -> groupElement
    groupElement
  }

  def env_clear = {
    $env = Map()
  }
  //  def env_setScriptName(scriptName:String) = {
  //    val $scriptName$ = "MyScript"
  //    $env += ScriptNameProp -> scriptName
  //  }
  def env_setBigInt(name:String, bigInt:BigInt) = {
    val $name$ = "b"
    val $bigInt$ = "123456789012345678901234567890123456789012345678901234567890"
    $env += name -> bigInt
  }
  def env_setInt(name:String, int:Int) = {
    val $name$ = "i"
    val $int$ = "123456789"
    $env += name -> int
  }
  def env_setLong(name:String, long:Long) = {
    val $name$ = "l"
    val $long$ = "123456789"
    $env += name -> long
  }
  def env_setCollByte(name:String, collBytes:Array[Byte]) = {
    val $name$ = "c"
    val $collBytes$ = "0x1a2b3c4d5e6f"
    $env += name -> collBytes
  }
  def env_setCollCollByte(name:String, collCollBytes:Array[Array[Byte]]) = {
    val $name$ = "d"
    val $collCollBytes$ = "[0x1a2b3c4d5e6f,0xafbecddc123,0xa132]"
    $env += name -> collCollBytes
  }

  def compile(ergoScript:Text):ErgoTree = {
    val $ergoScript$:String = "{\n  val x = blake2b256(c)\n\n  b == 1234.toBigInt &&\n  c == x &&\n  d(0) == x\n}"
    $compile(ergoScript.getText)
  }

  def abc(a:Text) = {
    val $a$ = "{\n  val x = blake2b256(c)\n  b == 1234.toBigInt &&\n  c == x &&\n  d(0) == x\n}"
    //val $x$ = "aaaaaa"

    //    val $x$:String =
    //      """{
    //        |  val x = blake2b256(c)
    //        |  b == 1234.toBigInt &&
    //        |  c == x &&
    //        |  d(0) == x
    //        |}
    //      """

    //a.getText.toString
    "Ok"
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
