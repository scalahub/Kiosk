package org.sh.kiosk.ergo

import java.security.SecureRandom

import org.ergoplatform.ErgoAddressEncoder.{MainnetNetworkPrefix, TestnetNetworkPrefix}
import org.ergoplatform.{ErgoAddressEncoder, Pay2SAddress, Pay2SHAddress}
import org.sh.cryptonode.ecc.{ECCPubKey, Point}
import org.sh.cryptonode.util.BytesUtil._
import org.sh.cryptonode.util.StringUtil._
import org.sh.easyweb.Text
import org.sh.reflect.DefaultTypeHandler
import sigmastate.Values.ErgoTree
import sigmastate.basics.SecP256K1
import sigmastate.eval.{CompiletimeIRContext, SigmaDsl}
import sigmastate.lang.SigmaCompiler
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer
import sigmastate.serialization.ValueSerializer
import special.collection.Coll
import special.sigma.GroupElement

object ErgoScriptUtil {
  def hexToGroupElement(hex:String) = {
    val point = ECCPubKey(hex).point
    val secp256k1Point = SecP256K1.createPoint(point.x.bigInteger, point.y.bigInteger)
    SigmaDsl.GroupElement(secp256k1Point)
  }

  def groupElementToHex(groupElement: GroupElement) = {
    groupElement.getEncoded.toArray.encodeHex
  }

  def hexToErgoTree(hex:String) = {
    val bytes = hex.decodeHex
    DefaultSerializer.deserializeErgoTree(bytes)
  }

  def ergoTreeTohex(tree:ErgoTree) = {
    DefaultSerializer.serializeErgoTree(tree).encodeHex
  }
}

import ErgoScriptUtil._

object ErgoScriptDemo extends ErgoScript {
  env_setBigInt("b", BigInt("123456789012345678901234567890123456789012345678901234567890"))
  env_setCollByte("c", "0x1a2b3c4d5e6f".decodeHex)
  env_setGroupElement("g", hexToGroupElement("028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"))

  def getPattern(ergoScript: Text, keysToMatch:Array[String]) = {
    val $keysToMatch$ = "[b, c]"
    val $ergoScript$ = """{
  val x = blake2b256(c)
  b == 1234.toBigInt &&
  c == x
}"""
    val scriptBytes = DefaultSerializer.serializeErgoTree($compile(ergoScript.getText))
    $matchScript(scriptBytes, keysToMatch: Array[String])
  }

}

abstract class ErgoScript {
  DefaultTypeHandler.addType[GroupElement](classOf[GroupElement], hexToGroupElement, groupElementToHex)
  DefaultTypeHandler.addType[ErgoTree](classOf[ErgoTree], hexToErgoTree, ergoTreeTohex)

  var $env:Map[String, Any] = Map()

  def $networkPrefix = if (ErgoAPI.$isMainNet) MainnetNetworkPrefix else TestnetNetworkPrefix
  def $compiler = SigmaCompiler($networkPrefix)
  implicit val $irContext = new CompiletimeIRContext
  implicit val $ergoAddressEncoder: ErgoAddressEncoder = new ErgoAddressEncoder($networkPrefix)

  def $arrByteToCollByte(a:Array[Byte]) = sigmastate.eval.Colls.fromArray(a)

  @deprecated("Unused as of now", "27 Aug 2019")
  def $arrArrByteToCollByte(a:Array[Array[Byte]]) = {
    val collArray = a.map{colByte =>
      sigmastate.eval.Colls.fromArray(colByte)
    }
    sigmastate.eval.Colls.fromArray(collArray)
  }

  //  def env_getSerialized = {
  //    $convertedEnv.toArray.map{
  //      case (name, value) =>
  //        val (elValue, elType) = value match {
  //          case grp: GroupElement => (ValueSerializer.serialize(grp), "GroupElement")
  //          case bigInt: special.sigma.BigInt => (ValueSerializer.serialize(bigInt), "BigInt")
  //          case bytes: Coll[Byte] => (ValueSerializer.serialize(???), "Coll[Byte]")
  //          case any => ???
  //        }
  //        s"""{"name":"$name", "value":"${elValue.encodeHex}", "type":"${elType}"}"""
  //    }
  //  }

  def env_get = {
    $env.toArray.map{
      case (name, value) =>
        val (elValue, elType) = value match {
          case grp: GroupElement => (grp.getEncoded.toArray.encodeHex, "GroupElement")
          case bigInt: BigInt => (bigInt.toString(10), "BigInt")
          // case bigInt: special.sigma.BigInt => (bigInt.toBytes.toArray.encodeHex, "BigInt")
          case collByte: Array[Byte] => (DefaultTypeHandler.typeToString(collByte.getClass, collByte), "Coll[Byte]")
          //case collCollByte: Array[Array[Byte]] => (DefaultTypeHandler.typeToString(collCollByte.getClass, collCollByte), "Coll[Coll[Byte]]")
          case any => (any.toString, any.getClass)
        }
        s"""{"name":"$name", "value":"${elValue}", "type":"${elType}"}"""
    }
  }

  def $getConvertedValue(value:Any) = {
    value match {
      case bigInt:BigInt => SigmaDsl.BigInt(bigInt.bigInteger)
      case collBytes:Array[Byte] => sigmastate.eval.Colls.fromArray(collBytes)
        /*
      case collCollBytes:Array[Array[Byte]] =>
        val collArray = collCollBytes.map{collBytes =>
          sigmastate.eval.Colls.fromArray(collBytes)
        }
        sigmastate.eval.Colls.fromArray(collArray)
         */
      case grp:GroupElement => grp
      case any => ???
    }
  }
  def $convertedEnv = {
    $env.map{
      case (key, value) => key -> $getConvertedValue(value)
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
  def env_setBigInt(name:String, bigInt:BigInt) = {
    val $name$ = "b"
    val $bigInt$ = "123456789012345678901234567890123456789012345678901234567890"
    $env += name -> bigInt
  }
  def env_setCollByte(name:String, collBytes:Array[Byte]) = {
    val $name$ = "c"
    val $collBytes$ = "0x1a2b3c4d5e6f"
    $env += name -> collBytes
  }

  /* // not fully tested

  def env_setCollCollByte(name:String, collCollBytes:Array[Array[Byte]]) = {
    val $name$ = "d"
    val $collCollBytes$ = "[0x1a2b3c4d5e6f,0xafbecddc12,0xa132]"
    $env += name -> collCollBytes
  }

  */

  def compile(ergoScript:Text):ErgoTree = {
    val $ergoScript$:String = """
{
  val x = blake2b256(c)
  b == 1234.toBigInt &&
  c == x
}"""
    $compile(ergoScript.getText)
  }

  def $getDefaultGenerator = {
    new ECCPubKey(org.sh.cryptonode.ecc.Util.G, true).hex
  }

  def $getGroupElement(exponent:BigInt) = {
    val g = SecP256K1.generator
    val h = SecP256K1.exponentiate(g, exponent.bigInteger).normalize()
    val x = h.getXCoord.toBigInteger
    val y = h.getYCoord.toBigInteger
    ECCPubKey(Point(x, y), true).hex
  }

  def $getRandomBigInt = {
    val random = new SecureRandom()
    val values = new Array[Byte](32)
    random.nextBytes(values)
    BigInt(values).mod(SecP256K1.q)
  }

  def getP2SH_Address(ergoScript:Text) = {
    val $ergoScript$ = """{
  val x = blake2b256(c)
  b == 1234.toBigInt &&
  c == x
}"""
    Pay2SHAddress(compile(ergoScript)).toString
  }

  def getP2S_Address(ergoScript:Text) = {
    val $ergoScript$ = """{
  val x = blake2b256(c)
  b == 1234.toBigInt &&
  c == x
}"""
    Pay2SAddress(compile(ergoScript)).toString
  }

  def getRandomKeyPair = {
    val prv = $getRandomBigInt
    Array("Private: "+prv.toString, "Public: "+$getGroupElement(prv))
  }

  def $matchScript(scriptBytes: Array[Byte], keysToMatch:Array[String]) = {
    val keyValsToMatch = keysToMatch.map{key =>
      val value = $convertedEnv.get(key).getOrElse(throw new Exception(s"Environment does not contain key $key"))
      key -> value
    }
    keyValsToMatch.foldLeft(scriptBytes.encodeHex)(
      (currStr, y) => {
        val (keyword, value) = y

        val serialized = value match {
          case grp: GroupElement => grp.getEncoded.toArray
          case bigInt: special.sigma.BigInt => bigInt.toBytes.toArray
          case collByte: Coll[Byte] => collByte.toArray
          case collCollByte: Coll[Coll[Byte]] => ??? // collCollByte.toArray.reduceLeft(_ ++ _):Array[Byte]
          case any =>
            println(s"ERROR [$keyword] => Any "+any+": "+any.getClass)
            ???
        }

        val encodedValue = serialized.encodeHex
        val value_r = encodedValue.length / 2
        val value_l = encodedValue.length - value_r
        val kw_r = keyword.length / 2
        val kw_l = keyword.length - kw_r
        val replacement = "<" + ("-" * (value_r - kw_r - 1)) + keyword + ("-" * (value_l - kw_l)) + ">"
        currStr.replace(encodedValue, replacement)
      }
    )

  }

}

