package kiosk.box

import java.util

import kiosk.appkit.Client
import kiosk.encoding.ScalaErgoConverters._
import kiosk.ergo._
import kiosk.script.KioskScriptCreator
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.{ErgoToken, InputBox, OutBox, OutBoxBuilder}
import org.sh.easyweb.Text
import org.sh.reflect.DataStructures.EasyMirrorSession
import org.sh.utils.json.JSONUtil.JsonFormatted
import sigmastate.Values.ErgoTree
import special.sigma.GroupElement
import scala.collection.mutable.{Map => MMap}

// ToDo: Add context variable to each box created
object KioskBoxCreator{
  private val sessionSecretBoxMap:MMap[String, (MMap[String, KioskBox], MMap[String, DhtData])] = MMap()

  private def boxMap(sessionSecret:Option[String]):(MMap[String, KioskBox], MMap[String, DhtData]) = {
    sessionSecret match {
      case None => (MMap(), MMap())
      case Some(secret) =>
        sessionSecretBoxMap.get(secret) match {
          case Some(map) => map
          case _ => sessionSecretBoxMap += secret -> (MMap(), MMap())
            sessionSecretBoxMap(secret)
        }
    }
  }
}


class KioskBoxCreator($ergoScript:KioskScriptCreator) extends EasyMirrorSession {
  import KioskBoxCreator._

  val $boxesDhts = boxMap($ergoScript.$myEnv.$sessionSecret)
  val $boxes = $boxesDhts._1
  val $dhts = $boxesDhts._2

  def getAll = {
    $boxes.map{
      case (name, box) =>
        new JsonFormatted {
          override val keys: Array[String] = Array("name") ++ box.keys
          override val vals: Array[Any] = Array(name) ++ box.vals
        }.toString
    }.toArray
  }

  def createBoxFromScript(boxName:String, script:Text, registerKeys:Array[String], tokenIDs:Array[String], tokenAmts:Array[Long], value:Long) = {
    val $INFO$ =
      """
1. Number of elements in the arrays tokenIDs and tokenAmts must be same. If you don't want to use tokens, set these array to empty (i.e., [])
2. registerKeys must refer to keys of ErgoEnv. Registers will be populated with the corresponding values starting with R4

As an example, to set R4 to Int 1 and R5 to Coll[Byte] 0x1234567890abcdef, first set these values in ErgoEnv using setInt and setCollByte
Let the keys for the Int and Coll[Byte] be, say, a and b respectively. Then set registerKeys value as [a,b]"""
    val $boxName$ = "box1"
    val $value$ = "123456"
    val $script$ = """{
  // Following values (among many others) can make this spendable
  //   a = 0xf091616c10378d94b04ed7afb6e7e8da3ec8dd2a9be4a343f886dd520f688563
  //   c = 0x1a2b3c4d5e6f
  //   b = any BigInt greater than 1234
  val x = blake2b256(c)
  sigmaProp(
     b > 1234.toBigInt
     &&
     a == x
  )
}"""
    val $registerKeys$ = "[a,b,c,d,g]"
    val $tokenIDs$ = "[]"
    val $tokenAmts$ = "[]"
    val ergoTree = $ergoScript.$compile(script)
    $create(boxName, ergoTree, registerKeys, tokenIDs, tokenAmts, value)
  }

  def createBoxFromAddress(boxName:String, address:String, registerKeys:Array[String], tokenIDs:Array[String], tokenAmts:Array[Long], value:Long) = {
    val $boxName$ = "box0"
    val $value$ = "123456"
    val $address$ =s"""box address"""
    val $INFO$ =
      """
1. Number of elements in the arrays tokenIDs and tokenAmts must be same. If you don't want to use tokens, set these array to empty (i.e., [])
2. registerKeys must refer to keys of ErgoEnv. Registers will be populated with the corresponding values starting with R4

As an example, to set R4 to Int 1 and R5 to Coll[Byte] 0x1234567890abcdef, first set these values in ErgoEnv using setInt and setCollByte
Let the keys for the Int and Coll[Byte] be, say, a and b respectively. Then set registerKeys value as [a,b]"""
    val ergoTree = getAddressFromString(address).script
    $create(boxName, ergoTree, registerKeys, tokenIDs, tokenAmts, value)
  }

  def $create(boxName:String, ergoTree:ErgoTree, registerKeys:Array[String], tokenIDs:Array[String], tokenAmts:Array[Long], value:Long) = {
    if ($boxes.contains(boxName)) throw new Exception(s"Name $boxName already exists. Use a different name")
    require(tokenIDs.size == tokenAmts.size, s"Number of tokenIDs (${tokenIDs.size}) does not match number of amounts (${tokenAmts.size})")
    val availableKeys = $ergoScript.$myEnv.$envMap.keys.foldLeft("")(_ + " "+ _)
    val registers = registerKeys.map{key =>
      val value: KioskType[_] = $ergoScript.$myEnv.$envMap.get(key).getOrElse(throw new Exception(s"Key $key not found in environment. Available keys [$availableKeys]"))
      value
    }
    val tokens:Tokens = tokenIDs zip tokenAmts
    val address = getStringFromAddress(getAddressFromErgoTree(ergoTree))
    val box = KioskBox(address, value, registers, tokens)
    $boxes += (boxName -> KioskBox(address, value, registers, tokens))
    box
  }

  def delete(boxName:String) = {
    if (!$boxes.contains(boxName)) throw new Exception(s"Name $boxName does not exist.")
    $boxes -= boxName
  }

  def deleteAll = {$boxes.clear()}

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

  def $dhtDataAdd(name:String, g:GroupElement, h:GroupElement, u:GroupElement, v:GroupElement, x:BigInt) = {
    $dhts += (name -> DhtData(g, h, u, v, x))
  }

  def $dhtDataClear = {
    $dhts.clear()
  }

  def $dhtDataGet = {
    $dhts.map{
      case (name, dht) => s"""{"name":"$name","g","${dht.g.hex},"h","${dht.h.hex},"u","${dht.u.hex},"v","${dht.v.hex}"}"""
    }
  }

  def createTx(inputBoxIds:Array[String], outputBoxNames:Array[String], fee:Long, changeAddress:String, proveDlogSecrets:Array[String], proveDhtDataNames:Array[String], broadcast:Boolean) = {
    val dhtData: Array[DhtData] = proveDhtDataNames.map($dhts(_))
    val boxesToCreate: Array[KioskBox] = outputBoxNames.map(outputBoxName => $boxes(outputBoxName))
    Client.usingClient{ctx =>
      val inputBoxes: Array[InputBox] = ctx.getBoxesById(inputBoxIds: _*)
      val txB = ctx.newTxBuilder
      val outputBoxes: Array[OutBox] = boxesToCreate.map{ box =>
        val outBoxBuilder: OutBoxBuilder = txB.outBoxBuilder().value(box.value).contract(
          new ErgoTreeContract(getAddressFromString(box.address).script)
        )
        val outBoxBuilderWithTokens: OutBoxBuilder = addTokens(outBoxBuilder)(box.tokens)
        val outBox: OutBox = addRegisters(outBoxBuilderWithTokens)(box.registers).build
        outBox
      }
      val inputs = new util.ArrayList[InputBox]()
      inputBoxes.foreach(inputs.add)
      val txToSign = ctx.newTxBuilder().boxesToSpend(inputs)
        .outputs(outputBoxes: _*)
        .fee(fee)
        .sendChangeTo(getAddressFromString(changeAddress)).build()

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

  override def $setSession(sessionSecret: Option[String]): KioskBoxCreator = new KioskBoxCreator($ergoScript.$setSession(sessionSecret))

}
