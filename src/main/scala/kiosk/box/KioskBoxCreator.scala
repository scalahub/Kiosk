package kiosk.box

import java.util

import kiosk.appkit.Client
import kiosk.encoding.ScalaErgoConverters
import kiosk.encoding.ScalaErgoConverters._
import kiosk.ergo._
import kiosk.script.KioskScriptCreator
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.{BlockchainContext, ErgoToken, InputBox, OutBox, OutBoxBuilder, SignedTransaction}
import org.sh.easyweb.Text
import org.sh.reflect.DataStructures.EasyMirrorSession
import org.sh.utils.json.JSONUtil.JsonFormatted
import sigmastate.Values.ErgoTree

import scala.collection.mutable.{Map => MMap}

// ToDo: Add context variable to each box created
object KioskBoxCreator {
  private val sessionSecretBoxMap: MMap[String, (MMap[String, KioskBox], MMap[String, DhtData])] = MMap()

  private def boxMap(
      sessionSecret: Option[String]
  ): (MMap[String, KioskBox], MMap[String, DhtData]) = {
    sessionSecret match {
      case None => (MMap(), MMap())
      case Some(secret) =>
        sessionSecretBoxMap.get(secret) match {
          case Some(map) => map
          case _ =>
            sessionSecretBoxMap += secret -> (MMap(), MMap())
            sessionSecretBoxMap(secret)
        }
    }
  }
}

class KioskBoxCreator(val $ergoScript: KioskScriptCreator) extends EasyMirrorSession {
  import KioskBoxCreator._

  val $boxesDhts = boxMap($ergoScript.$myEnv.$sessionSecret)
  val $boxes = $boxesDhts._1
  val $dhts = $boxesDhts._2

  def getAllBoxes = {
    $boxes.map {
      case (name, box) =>
        new JsonFormatted {
          override val keys: Array[String] = Array("name") ++ box.keys
          override val vals: Array[Any] = Array(name) ++ box.vals
        }.toString
    }.toArray
  }

  def createBoxFromScript(boxName: String, script: Text, registerKeys: Array[String], tokenIDs: Array[String], tokenAmts: Array[Long], value: Long) = {
    val $INFO$ =
      """
1. Number of elements in the arrays tokenIDs and tokenAmts must be same. If you don't want to use tokens, set these array to empty (i.e., [])
2. registerKeys must refer to keys of Env. Registers will be populated with the corresponding values starting with R4

As an example, to set R4 to Int 1 and R5 to Coll[Byte] 0x1234567890abcdef, first set these values in Env using setInt and setCollByte
Let the keys for the Int and Coll[Byte] be, say, a and b respectively. Then set registerKeys value as [a,b]"""
    val $boxName$ = "mySecondBox"
    val $value$ = "123456"
    val $script$ = """{
  sigmaProp(1 < 2)
}"""
    val ergoTree = $ergoScript.$compile(script)
    $create(boxName, ergoTree, registerKeys, tokenIDs, tokenAmts, value)
  }

  def createBoxFromAddress(boxName: String, address: String, registerKeys: Array[String], tokenIDs: Array[String], tokenAmts: Array[Long], value: Long) = {
    val $boxName$ = "myFirstBox"
    val $value$ = "123456"
    val $address$ = s"""4MQyML64GnzMxZgm"""
    val $INFO$ =
      """
1. Number of elements in the arrays tokenIDs and tokenAmts must be same. If you don't want to use tokens, set these array to empty (i.e., [])
2. registerKeys must refer to keys of Env. Registers will be populated with the corresponding values starting with R4

As an example, to set R4 to Int 1 and R5 to Coll[Byte] 0x1234567890abcdef, first set these values in Env using setInt and setCollByte
Let the keys for the Int and Coll[Byte] be, say, a and b respectively. Then set registerKeys value as [a,b]
The default address 4MQyML64GnzMxZgm corresponds to the script {1 < 2}"""
    val ergoTree = getAddressFromString(address).script
    $create(boxName, ergoTree, registerKeys, tokenIDs, tokenAmts, value)
  }

  def $create(boxName: String, ergoTree: ErgoTree, registerKeys: Array[String], tokenIDs: Array[String], tokenAmts: Array[Long], value: Long) = {
    if ($boxes.contains(boxName))
      throw new Exception(s"Name $boxName already exists. Use a different name")
    require(
      tokenIDs.size == tokenAmts.size,
      s"Number of tokenIDs (${tokenIDs.size}) does not match number of amounts (${tokenAmts.size})"
    )
    val availableKeys =
      $ergoScript.$myEnv.$envMap.keys.foldLeft("")(_ + " " + _)
    val registers = registerKeys.map { key =>
      val value: KioskType[_] = $ergoScript.$myEnv.$envMap.get(key).getOrElse(
        throw new Exception(
          s"Key $key not found in environment. Available keys [$availableKeys]"
        )
      )
      value
    }
    val tokens: Tokens = tokenIDs zip tokenAmts
    val address = getStringFromAddress(getAddressFromErgoTree(ergoTree))
    val box = KioskBox(address, value, registers, tokens)
    $boxes += (boxName -> KioskBox(address, value, registers, tokens))
    box
  }

  def $deleteBox(boxName: String) = {
    if (! $boxes.contains(boxName))
      throw new Exception(s"Name $boxName does not exist.")
    $boxes -= boxName
  }

  def deleteAllBoxes(reallyDelete: Boolean) = {
    val $INFO$ =
      "To prevent accidental clicking, please select 'yes' from the radio button"
    val $reallyDelete$ = "false"
    if (reallyDelete) {
      $boxes.clear()
      "Deleted all boxes"
    } else {
      "Please set reallyDelete to yes to delete all boxes"
    }
  }

  private def addTokens(outBoxBuilder: OutBoxBuilder)(tokens: Seq[Token]) = {
    if (tokens.isEmpty) outBoxBuilder
    else {
      outBoxBuilder.tokens(tokens.map { token =>
        val (id, value) = token
        new ErgoToken(id, value)
      }: _*)
    }
  }

  private def addRegisters(
      outBoxBuilder: OutBoxBuilder
  )(registers: Array[KioskType[_]]) = {
    if (registers.isEmpty) outBoxBuilder
    else {
      outBoxBuilder.registers(registers.map(_.getErgoValue): _*)
    }
  }

  def dhtDataAdd(name: String, g: String, h: String, u: String, v: String, x: BigInt): Unit = {
    val $name$ = "dht1"
    val $g$ =
      "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"
    val $h$ =
      "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"
    val $u$ =
      "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"
    val $v$ =
      "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"
    val $x$ = "1"

    implicit def str2GrpElem(s: String) =
      ScalaErgoConverters.stringToGroupElement(s)
    $dhts.get(name).fold($dhts += (name -> DhtData(g, h, u, v, x)))(
      _ =>
        throw new Exception(
          s"DHTuple ${name} is already defined. Use a different name"
      )
    )

  }

  def dhtDataDeleteAll(reallyDelete: Boolean) = {
    val $INFO$ =
      "To prevent accidental clicking, please select 'yes' from the radio button"
    val $reallyDelete$ = "false"
    if (reallyDelete) {
      $dhts.clear()
      "Deleted all DHT data"
    } else {
      "Please set reallyDelete to yes to delete all DHT data"
    }
  }

  def dhtDataGetAll = {
    $dhts.map {
      case (name, dht) =>
        new JsonFormatted {
          override val keys: Array[String] = dht.keys :+ "name"
          override val vals: Array[Any] = dht.vals :+ name
        }
    }
  }

  def $createTx(inputBoxes: Array[InputBox],
                dataInputs: Array[InputBox],
                boxesToCreate: Array[KioskBox],
                fee: Long,
                changeAddress: String,
                proveDlogSecrets: Array[String],
                dhtData: Array[DhtData],
                broadcast: Boolean)(implicit ctx: BlockchainContext): SignedTransaction = {
    val txB = ctx.newTxBuilder
    val outputBoxes: Array[OutBox] = boxesToCreate.map { box =>
      val outBoxBuilder: OutBoxBuilder = txB
        .outBoxBuilder()
        .value(box.value)
        .contract(
          new ErgoTreeContract(getAddressFromString(box.address).script)
        )
      val outBoxBuilderWithTokens: OutBoxBuilder =
        addTokens(outBoxBuilder)(box.tokens)
      val outBox: OutBox =
        addRegisters(outBoxBuilderWithTokens)(box.registers).build
      outBox
    }
    val inputs = new util.ArrayList[InputBox]()

    inputBoxes.foreach(inputs.add)

    val dataInputBoxes = new util.ArrayList[InputBox]()

    dataInputs.foreach(dataInputBoxes.add)

    val txToSign = ctx
      .newTxBuilder()
      .boxesToSpend(inputs)
      .withDataInputs(dataInputBoxes)
      .outputs(outputBoxes: _*)
      .fee(fee)
      .sendChangeTo(getAddressFromString(changeAddress))
      .build()

    val dlogProver = proveDlogSecrets.foldLeft(ctx.newProverBuilder()) {
      case (oldProverBuilder, newDlogSecret) =>
        oldProverBuilder.withDLogSecret(BigInt(newDlogSecret).bigInteger)
    }

    val dhtProver = dhtData.foldLeft(dlogProver) {
      case (oldProverBuilder, dht) =>
        oldProverBuilder.withDHTData(
          dht.g,
          dht.h,
          dht.u,
          dht.v,
          dht.x.bigInteger
        )
    }

    val signedTx = dhtProver.build().sign(txToSign)
    if (broadcast) ctx.sendTransaction(signedTx)
    signedTx
  }

  def createTx(inputBoxIds: Array[String],
               dataInputBoxIds: Array[String],
               outputBoxNames: Array[String],
               fee: Long,
               changeAddress: String,
               proveDlogSecrets: Array[String],
               proveDhtDataNames: Array[String],
               broadcast: Boolean) = {
    val dhtData: Array[DhtData] = proveDhtDataNames.map($dhts(_))
    val boxesToCreate: Array[KioskBox] = outputBoxNames.map(outputBoxName => $boxes(outputBoxName))
    Client.usingClient { implicit ctx =>
      val inputBoxes: Array[InputBox] = ctx.getBoxesById(inputBoxIds: _*)
      val dataInputBoxes: Array[InputBox] = ctx.getBoxesById(dataInputBoxIds: _*)
      $createTx(inputBoxes, dataInputBoxes, boxesToCreate, fee, changeAddress, proveDlogSecrets, dhtData, broadcast).toJson(false)
    }
  }

  override def $setSession(sessionSecret: Option[String]): KioskBoxCreator =
    new KioskBoxCreator($ergoScript.$setSession(sessionSecret))

}
