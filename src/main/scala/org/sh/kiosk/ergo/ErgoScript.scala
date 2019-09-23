package org.sh.kiosk.ergo

import java.security.SecureRandom

import org.ergoplatform.ErgoAddressEncoder.{MainnetNetworkPrefix, TestnetNetworkPrefix}
import org.ergoplatform.{ErgoAddressEncoder, Pay2SAddress, Pay2SHAddress}
import org.json.JSONObject
import org.sh.cryptonode.ecc.{ECCPubKey, Point}
import org.sh.cryptonode.util.BytesUtil._
import org.sh.cryptonode.util.StringUtil._
import sigmastate.Values.ErgoTree
import sigmastate.basics.SecP256K1
import sigmastate.eval.CompiletimeIRContext
import sigmastate.lang.SigmaCompiler
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer
import special.sigma.GroupElement
import org.sh.kiosk.ergo.util.ErgoScriptUtil._
import special.collection.Coll
import org.sh.easyweb.Text
import org.sh.reflect.DefaultTypeHandler
import org.sh.utils.json.JSONUtil.JsonFormatted
import sigmastate.serialization.ValueSerializer

object ErgoScriptDemo extends ErgoScript {
  env_setBigInt("b", BigInt("123456789012345678901234567890123456789012345678901234567890"))
  env_setCollByte("c", "0x1a2b3c4d5e6f".decodeHex)
  env_setGroupElement("g", hexToGroupElement("028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"))

  def getPattern(ergoScript: Text, keysToMatch:Array[String], useRegex:Boolean) = {
    val $useRegex$ = "false"
    val $keysToMatch$ = "[b, c]"
    val $ergoScript$ = """{
  val x = blake2b256(c)
  b == 1234.toBigInt &&
  c == x
}"""
    val f:(Array[Byte], Array[String]) => String = if (useRegex) $regex else $matchScript

    f(DefaultSerializer.serializeErgoTree($compile(ergoScript.getText)), keysToMatch: Array[String])
  }

}

abstract class ErgoScript {
  DefaultTypeHandler.addType[GroupElement](classOf[GroupElement], hexToGroupElement, groupElementToHex)
  DefaultTypeHandler.addType[ErgoTree](classOf[ErgoTree], hexToErgoTree, ergoTreeToHex)

  // any variable/method starting with $ will not appear in front-end.
  // so any variable to be hidden from front-end is prefixed with $

  private var $scala_env:Map[String, Any] = Map()

  def $networkPrefix = if (ErgoAPI.$isMainNet) MainnetNetworkPrefix else TestnetNetworkPrefix
  def $compiler = SigmaCompiler($networkPrefix)
  implicit val $irContext = new CompiletimeIRContext
  implicit val $ergoAddressEncoder: ErgoAddressEncoder = new ErgoAddressEncoder($networkPrefix)

  def $arrByteToCollByte(a:Array[Byte]) = sigmastate.eval.Colls.fromArray(a)

  @deprecated("Unused as of now", "27 Aug 2019")
  def $arrArrByteToCollCollByte(a:Array[Array[Byte]]) = {
    val collArray = a.map{colByte =>
      sigmastate.eval.Colls.fromArray(colByte)
    }
    sigmastate.eval.Colls.fromArray(collArray)
  }

  def env_get = {
    $scala_env.toArray.map{
      case (name, value) =>
        val (elValue, elType) = value match {
          case grp: GroupElement => (grp.getEncoded.toArray.encodeHex, "GroupElement")
          case bigInt: BigInt => (bigInt.toString(10), "BigInt")
          case arrayByte: Array[Byte] => (DefaultTypeHandler.typeToString(arrayByte.getClass, arrayByte), "Coll[Byte]")
          case arrayArrayByte: Array[Array[Byte]] => (DefaultTypeHandler.typeToString(arrayArrayByte.getClass, arrayArrayByte), "Coll[Coll[Byte]]")
          case any => (any.toString, any.getClass)
        }
        s"""{"name":"$name", "value":"${elValue}", "type":"${elType}"}"""
    }
  }

  def $env = {
    // use def because $scala_env can be modified anytime and we need to use the latest one
    $scala_env.map{ case (key, value) => key -> getConvertedValue(value) }
  }

  def $compile(ergoScript:String):ErgoTree = {
    import sigmastate.lang.Terms._
    $compiler.compile($env, ergoScript).asSigmaProp
  }

  def env_setGroupElement(name:String, groupElement: GroupElement) = {
    val $INFO$ = "A group element is encoded as a public key of Bitcoin in hex (compressed or uncompressed)"
    val $name$ = "g"
    val $groupElement$ = "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"
    $scala_env += name -> groupElement
    groupElement
  }

  def env_clear = {
    $scala_env = Map()
  }
  def env_setBigInt(name:String, bigInt:BigInt) = {
    val $name$ = "b"
    val $bigInt$ = "123456789012345678901234567890123456789012345678901234567890"
    $scala_env += name -> bigInt
  }
  def env_setLong(name:String, long:Long) = {
    val $name$ = "long"
    val $long$ = "12345678901112"
    $scala_env += name -> long
  }
  def env_setInt(name:String, int:Int) = {
    val $name$ = "long"
    val $int$ = "123456789"
    $scala_env += name -> int
  }
  def env_setCollByte(name:String, collBytes:Array[Byte]) = {
    val $name$ = "c"
    val $collBytes$ = "0x1a2b3c4d5e6f"
    $scala_env += name -> collBytes
  }

  @deprecated("Not fully supported")
  def $scala_env_setCollCollByte(name:String, collCollBytes:Array[Array[Byte]]) = {
    val $name$ = "d"
    val $collCollBytes$ = "[0x1a2b3c4d5e6f,0xafbecddc12,0xa132]"
    $scala_env += name -> collCollBytes
  }

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


  def getRandomKeyPair = {
    val prv = getRandomBigInt
    Array("Private: "+prv.toString, "Public: "+$getGroupElement(prv))
  }
  def $getGroupElement(exponent:BigInt) = {
    val g = SecP256K1.generator
    val h = SecP256K1.exponentiate(g, exponent.bigInteger).normalize()
    val x = h.getXCoord.toBigInteger
    val y = h.getYCoord.toBigInteger
    ECCPubKey(Point(x, y), true).hex
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

  def $getKeysFromEnv(keys:Array[String]) = {
    keys.map{key =>
      val value = $env.get(key).getOrElse(throw new Exception(s"Environment does not contain key $key"))
      key -> value
    }
  }
  def $regex(scriptBytes: Array[Byte], keysToMatch:Array[String]) = {
    val hex = scriptBytes.encodeHex
    val startRegex = s"^$hex$$"
    $getKeysFromEnv(keysToMatch).foldLeft(startRegex)(
      (currRegex, y) => {
        val (keyword, value) = y
        val serialized:Array[Byte] = serialize(value)
        val encodedValue = serialized.encodeHex
        val replacement = s"[0-9a-fA-F]{${encodedValue.size}}"
        currRegex.replace(encodedValue, replacement)
      }
    )
  }
  def $matchScript(scriptBytes: Array[Byte], keysToMatch:Array[String]):String = {
    $getKeysFromEnv(keysToMatch).foldLeft(scriptBytes.encodeHex)(
      (currStr, y) => {
        val (keyword, value) = y

        val serialized:Array[Byte] = serialize(value)
        val encodedValue = serialized.encodeHex
        val value_r = encodedValue.length / 2
        val value_l = encodedValue.length - value_r
        val kw_r = keyword.length / 2
        val kw_l = keyword.length - kw_r
        val replacement = "<" + ("-" * (value_r - kw_r - 1)) + keyword + ("-" * (value_l - kw_l-1)) + ">"
        currStr.replace(encodedValue, replacement)
      }
    )
  }

  var $boxes:Map[String, Box] = Map() // boxName -> Box

  def box_delete(boxName:String) = {
    if (!$boxes.contains(boxName)) throw new Exception(s"Name $boxName does not exist.")
    $boxes -= boxName
  }

  def box_deleteAll = {$boxes = Map()}

  def box_create(boxName:String, ergoScript:Text, registerKeys:Array[String], tokenIDs:Array[Array[Byte]], tokenAmts:Array[Long]) = {
    val $boxName$ = "box1"
    val $ergoScript$ = """{
  val x = blake2b256(c)
  b == 1234.toBigInt &&
  c == x
}"""
    val $registerKeys$ = "[b,c]"
    val $tokenIDs$ = "[]"
    val $tokenAmts$ = "[]"

    if ($boxes.contains(boxName)) throw new Exception(s"Name $boxName already exists. Use a different name")
    require(tokenIDs.size == tokenAmts.size, s"Number of tokenIDs (${tokenIDs.size}) does not match number of amounts (${tokenAmts.size})")
    val availableKeys = $scala_env.keys.foldLeft("")(_ + " "+ _)
    val registers:Registers = registerKeys.map{key =>
      val value = $env.get(key).getOrElse(throw new Exception(s"Key $key not found in environment. Available keys [$availableKeys]"))
      serialize(value)
    }
    val tokens:Tokens = tokenIDs zip tokenAmts
    $boxes += (boxName -> Box(compile(ergoScript), registers, tokens))
  }

  def tx_create(inBoxBytes:Array[Array[Byte]], outBoxNames:Array[String]) = {
    val $inBoxBytes$ = "[]"
    val $outBoxNames$ = "[box1]"
    val outBoxes = outBoxNames.map{boxName =>
      $boxes.get(boxName).getOrElse(throw new Exception(s"No such box $boxName"))
    }

  }

  def box_getAll: Array[JsonFormatted] = {
    $boxes.map{
      case (name, box) =>
      new JsonFormatted {
        override val keys: Array[String] = Array("name") ++ box.keys
        override val vals: Array[Any] = Array(name) ++ box.vals
      }
    }.toArray
  }

  def box_get(boxName:String) = {
    $boxes.get(boxName)
  }

}

