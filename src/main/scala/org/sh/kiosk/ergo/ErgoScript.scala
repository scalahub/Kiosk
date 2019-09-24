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

object ErgoEnv extends Env
object PlayGround extends ErgoScript(ErgoEnv) {
  ErgoEnv.setBigInt("b", BigInt("123456789012345678901234567890123456789012345678901234567890"))
  ErgoEnv.setCollByte("c", "0x1a2b3c4d5e6f".decodeHex)
  ErgoEnv.setGroupElement("g", hexToGroupElement("028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"))

  def $getPattern(ergoScript: Text, keysToMatch:Array[String], useRegex:Boolean) = {
    val $useRegex$ = "false"
    val $keysToMatch$ = "[b, c]"
    val $ergoScript$ = """{
  val x = blake2b256(c)
  b == 1234.toBigInt &&
  c == x
}"""
    val f:(Array[Byte], Array[String]) => String = if (useRegex) $myEnv.$regex else $myEnv.$matchScript

    f(DefaultSerializer.serializeErgoTree($compile(ergoScript.getText)), keysToMatch: Array[String])
  }

}

class ErgoScript(val $myEnv:Env) {
  // any variable/method starting with $ will not appear in front-end.
  // so any variable to be hidden from front-end is prefixed with $
  import $myEnv._
  def $networkPrefix = if (API.$isMainNet) MainnetNetworkPrefix else TestnetNetworkPrefix
  def $compiler = SigmaCompiler($networkPrefix)
  implicit val $irContext = new CompiletimeIRContext
  implicit val $ergoAddressEncoder: ErgoAddressEncoder = new ErgoAddressEncoder($networkPrefix)

  def box_create(boxName:String, ergoScript:Text, registerKeys:Array[String], tokenIDs:Array[Array[Byte]], tokenAmts:Array[Long], useP2S:Boolean, value:Long) = {
    val $INFO$ = "If use P2S is false then it will use P2SH address"
    val $boxName$ = "box1"
    val $useP2S$ = "false"
    val $value$ = "10000"
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
    val availableKeys = $myEnv.$scala_env.keys.foldLeft("")(_ + " "+ _)
    val registers:Registers = registerKeys.map{key =>
      val value = $myEnv.$getEnv.get(key).getOrElse(throw new Exception(s"Key $key not found in environment. Available keys [$availableKeys]"))
      serialize(value)
    }
    val tokens:Tokens = tokenIDs zip tokenAmts
    val ergoTree = $compile(ergoScript)
    val address = if (useP2S) Pay2SAddress(ergoTree).toString else  Pay2SHAddress(ergoTree).toString
    $boxes += (boxName -> Box(address, value, registers, tokens))
  }


  def $compile(ergoScript:String):ErgoTree = {
    import sigmastate.lang.Terms._
    $compiler.compile($myEnv.$getEnv, ergoScript).asSigmaProp
  }

  def $compile(ergoScript:Text):ErgoTree = {
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


  def $getRandomKeyPair = {
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

  def $getP2SH_Address(ergoScript:Text) = {
    val $ergoScript$ = """{
  val x = blake2b256(c)
  b == 1234.toBigInt &&
  c == x
}"""
    Pay2SHAddress($compile(ergoScript)).toString
  }

  def $getP2S_Address(ergoScript:Text) = {
    val $ergoScript$ = """{
  val x = blake2b256(c)
  b == 1234.toBigInt &&
  c == x
}"""
    Pay2SAddress($compile(ergoScript)).toString
  }

  def $getKeysFromEnv(keys:Array[String]) = {
    keys.map{key =>
      val value = $myEnv.$getEnv.get(key).getOrElse(throw new Exception(s"Environment does not contain key $key"))
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

  def tx_create(inBoxBytes:Array[Array[Byte]], outBoxNames:Array[String]) = {
    val $inBoxBytes$ = "[]"
    val $outBoxNames$ = "[box1]"
    val outBoxes = outBoxNames.map{boxName =>
      $boxes.get(boxName).getOrElse(throw new Exception(s"No such box $boxName"))
    }

    /*
{
  "requests": [
    {
      "address": "3WwbzW6u8hKWBcL1W7kNVMr25s2UHfSBnYtwSHvrRQt7DdPuoXrt",
      "value": 1,
      "assets": [
        {
          "tokenId": "4ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
          "amount": 1000
        }
      ],
      "registers": {
        "R4": "100204a00b08cd0336100ef59ced80ba5f89c4178ebd57b6c1dd0f3d135ee1db9f62fc634d637041ea02d192a39a8cc7a70173007301"
      }
    }, {...}
    ]

     */

    //case class Box(address:String, value:Long, registers: Registers, tokens: Tokens) extends JsonFormatted {
    def registerJson(id:Int, register:Register) = {
      s""""R$id":"${register.encodeHex}""""
    }

    def assetStr(token:Token):String = {
      val (id, amt) = token
      s"""{"tokenId":"${id.encodeHex}","amount":$amt}""".stripMargin
    }

    def getBoxJson(b:Box) = {
      val assetJson = b.tokens.map(assetStr).mkString(",")
      val registersJson = b.registers.zipWithIndex.map{case (data, id) => registerJson(id+4, data)}.mkString(",")
      s"""{"address":"${b.address}","value":${b.value},"assets":[$assetJson],"registers":{$registersJson}}""".stripMargin
    }

    val request = outBoxes.map(getBoxJson ).mkString(",")
    val json = s"""{"requests":[$request],"inputsRaw":[]}"""

    val resp = API.$q("wallet/transaction/generate", true, PostJsonRaw, Nil, Some(json))
    Array(resp, json)
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

