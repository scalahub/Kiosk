package org.sh.kiosk.ergo.box

import java.util

import org.ergoplatform.Pay2SAddress
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.{ErgoToken, InputBox, OutBox, OutBoxBuilder}
import org.sh.easyweb.Text
import org.sh.kiosk.ergo
import org.sh.kiosk.ergo.appkit.Client
import org.sh.kiosk.ergo.script.ErgoScript
import org.sh.kiosk.ergo.script.ErgoScript.$ergoAddressEncoder
import org.sh.kiosk.ergo._
import org.sh.utils.json.JSONUtil.JsonFormatted
import special.sigma.GroupElement

// ToDo: Add context variable to each box created
class ErgoBox($ergoScript:ErgoScript) {
  var $boxes:Map[String, Box] = Map() // boxName -> Box
  var $dhts:Map[String, DhtData] = Map() // dhtDataName -> DhtData
  def getAll: Array[JsonFormatted] = {
    $boxes.map{
      case (name, box) =>
        new JsonFormatted {
          override val keys: Array[String] = Array("name") ++ box.keys
          override val vals: Array[Any] = Array(name) ++ box.vals
        }
    }.toArray
  }

  def create(boxName:String, script:Text, registerKeys:Array[String], tokenIDs:Array[String], tokenAmts:Array[Long], value:Long) = {
    val $INFO$ =
      """
1. Number of elements in the arrays tokenIDs and tokenAmts must be same. If you don't want to use tokens, set these array to empty (i.e., [])
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
    val availableKeys = $ergoScript.$myEnv.$envMap.keys.foldLeft("")(_ + " "+ _)
    val registers = registerKeys.map{key =>
      val value: ergo.KioskType[_] = $ergoScript.$myEnv.$envMap.get(key).getOrElse(throw new Exception(s"Key $key not found in environment. Available keys [$availableKeys]"))
      value
    }
    val tokens:Tokens = tokenIDs zip tokenAmts
    val ergoTree = $ergoScript.compile(script)
    val address = Pay2SAddress(ergoTree).toString
    val box = Box(address, value, registers, tokens)
    $boxes += (boxName -> Box(address, value, registers, tokens))
    box
  }

  def delete(boxName:String) = {
    if (!$boxes.contains(boxName)) throw new Exception(s"Name $boxName does not exist.")
    $boxes -= boxName
  }

  def deleteAll = {$boxes = Map()}

  private def addTokens(outBoxBuilder: OutBoxBuilder)(tokens:Seq[Token]) = {
    if (tokens.isEmpty) outBoxBuilder else {
      outBoxBuilder.tokens(
        tokens.map{token =>
          val (id, value) = token
          new ErgoToken(id, value)
        }: _*
      )
    }
  }

  private def addRegisters(outBoxBuilder: OutBoxBuilder)(registers:Array[KioskType[_]]) = {
    if (registers.isEmpty) outBoxBuilder else {
      outBoxBuilder.registers(registers.map(_.getErgoValue): _*)
    }
  }

  def dhtDataAdd(name:String, g:GroupElement, h:GroupElement, u:GroupElement, v:GroupElement, x:BigInt) = {
    $dhts += (name -> DhtData(g, h, u, v, x))
  }

  def dhtDataClear = {
    $dhts = Map()
  }

  def dhtDataGet = {
    $dhts.map{
      case (name, dht) => s"""{"name":"$name","g","${dht.g.hex},"h","${dht.h.hex},"u","${dht.u.hex},"v","${dht.v.hex}"}"""
    }
  }

  def createTx(inputBoxIds:Array[String], outputBoxNames:Array[String], fee:Long, changeAddress:String, proveDlogSecrets:Array[String], proveDhtDataNames:Array[String], broadcast:Boolean) = {
    val dhtData: Array[DhtData] = proveDhtDataNames.map($dhts(_))
    val boxesToCreate: Array[Box] = outputBoxNames.map(outputBoxName => $boxes(outputBoxName))
    Client.usingClient{ctx =>
      val inputBoxes: Array[InputBox] = ctx.getBoxesById(inputBoxIds: _*)
      val txB = ctx.newTxBuilder
      val outputBoxes: Array[OutBox] = boxesToCreate.map{ b =>
        val outBoxBuilder: OutBoxBuilder = txB.outBoxBuilder().value(b.value).contract(
          new ErgoTreeContract(ergo.getAddressFromString(b.address).script)
        )
        val outBoxBuilderWithTokens: OutBoxBuilder = addTokens(outBoxBuilder)(b.tokens)
        val outBox: OutBox = addRegisters(outBoxBuilderWithTokens)(b.registers).build
        outBox
      }
      val inputs = new util.ArrayList[InputBox]()
      inputBoxes.foreach(inputs.add)
      val txToSign = ctx.newTxBuilder().boxesToSpend(inputs)
        .outputs(outputBoxes: _*)
        .fee(fee)
        .sendChangeTo(ergo.getAddressFromString(changeAddress)).build()

      val dlogProver = proveDlogSecrets.foldLeft(ctx.newProverBuilder()){
        case (oldProverBuilder, newDlogSecret) => oldProverBuilder.withDLogSecret(BigInt(newDlogSecret).bigInteger)
      }

      val dhtProver = dhtData.foldLeft(dlogProver){
        case (oldProverBuilder, dht) => oldProverBuilder.withDHTData(dht.g, dht.h, dht.u, dht.v, dht.x.bigInteger)
      }

      val signedTx = dhtProver.build().sign(txToSign)
      if (broadcast) ctx.sendTransaction(signedTx)
      signedTx.toJson(false)
    }
  }
}
