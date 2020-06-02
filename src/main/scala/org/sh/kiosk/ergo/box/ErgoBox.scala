package org.sh.kiosk.ergo.box

import org.ergoplatform.Pay2SAddress
import org.sh.easyweb.Text
import org.sh.cryptonode.util.BytesUtil._
import org.sh.kiosk.ergo.encoding.ScalaErgoConverters
import org.sh.kiosk.ergo.fullnode.API
import org.sh.kiosk.ergo.fullnode.ReqType.PostJsonRaw
import org.sh.kiosk.ergo.script.ErgoScript
import org.sh.kiosk.ergo.script.ErgoScript.$ergoAddressEncoder
import org.sh.kiosk.ergo.{Box, Register, Registers, Token, Tokens}
import org.sh.utils.json.JSONUtil.JsonFormatted

class ErgoBox($ergoScript:ErgoScript) {
  var $boxes:Map[String, Box] = Map() // boxName -> Box

  def boxGetAll: Array[JsonFormatted] = {
    $boxes.map{
      case (name, box) =>
        new JsonFormatted {
          override val keys: Array[String] = Array("name") ++ box.keys
          override val vals: Array[Any] = Array(name) ++ box.vals
        }
    }.toArray
  }

  def boxGet(boxName:String) = {
    $boxes.get(boxName)
  }

  def boxCreate(boxName:String, script:Text, registerKeys:Array[String], tokenIDs:Array[Array[Byte]], tokenAmts:Array[Long], value:Long) = {
    val $INFO$ =
      """
1. Number of elements in the arrays tokenIDs and tokenAmts must be same. If you don't want to use tokens, set these arrya to empty (i.e., [])
2. registerKeys must refer to keys of ErgoEnv. Registers will be populated with the corresponding values starting with R4

As an example, to set R4 to Int 1 and R5 to Coll[Byte] 0x1234567890abcdef, first set these values in ErgoEnv using setInt and setCollByte
Let the keys for the Int and Coll[Byte] be, say, a and b respectively. Then set registerKeys value as [a,b]"""
    val $boxName$ = "box1"
    val $useP2S$ = "false"
    val $value$ = "123456"
    val $ergoScript$ = """{
  // Following values (among many others) can make this spendable
  //   a = 0xf091616c10378d94b04ed7afb6e7e8da3ec8dd2a9be4a343f886dd520f688563
  //   c = 0x1a2b3c4d5e6f
  //   b = any BigInt greater than 1234
  val x = blake2b256(c)
  b > 1234.toBigInt &&
  a == x
}"""
    val $registerKeys$ = "[a,b,c]"
    val $tokenIDs$ = "[]"
    val $tokenAmts$ = "[]"

    if ($boxes.contains(boxName)) throw new Exception(s"Name $boxName already exists. Use a different name")
    require(tokenIDs.size == tokenAmts.size, s"Number of tokenIDs (${tokenIDs.size}) does not match number of amounts (${tokenAmts.size})")
    val availableKeys = $ergoScript.$myEnv.$scala_env.keys.foldLeft("")(_ + " "+ _)
    val registers:Registers = registerKeys.map{key =>
      val value = $ergoScript.$myEnv.$getEnv.get(key).getOrElse(throw new Exception(s"Key $key not found in environment. Available keys [$availableKeys]"))
      ScalaErgoConverters.serialize(value)
    }
    val tokens:Tokens = tokenIDs zip tokenAmts
    val ergoTree = $ergoScript.compile(script)
    val address = Pay2SAddress(ergoTree).toString
    val box = Box(address, value, registers, tokens)
    $boxes += (boxName -> Box(address, value, registers, tokens))
    box
  }

  def boxDelete(boxName:String) = {
    if (!$boxes.contains(boxName)) throw new Exception(s"Name $boxName does not exist.")
    $boxes -= boxName
  }

  def boxDeleteAll = {$boxes = Map()}

  def createTx(inBoxBytes:Array[Array[Byte]],
                    outBoxNames:Array[String], generateOnly:Boolean) = {
    val $INFO$ = """
    This method will generate and optionally also send an Ergo transaction.
    If `usePaymentSend` is true, endpoint is /wallet/payment/send otherwise it is /wallet/transaction/send
    If `generateOnly` is true, it will output a single row with the JSON to be used at the endpoint
    If `generateOnly` is false, it will output two rows. The first is output of calling the send method. The second is the JSON used as input for the send method.
    """
    val $inBoxBytes$ = "[]"
    val $outBoxNames$ = "[box1]"
    val outBoxes = outBoxNames.map{boxName =>
      $boxes.get(boxName).getOrElse(throw new Exception(s"No such box $boxName"))
    }

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
    val requestJson = s"""[$request]"""

    if (generateOnly) {
      Array(requestJson)
    } else {
      val resp = API.nodeQuery("/wallet/payment/send", true, PostJsonRaw, Array.empty, Array.empty, Some(requestJson))
      Array(resp, requestJson)
    }
  }

}
