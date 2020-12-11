package kiosk.wallet

import kiosk.appkit.Client
import kiosk.box.KioskBoxCreator
import kiosk.encoding.EasyWebEncoder
import kiosk.ergo
import kiosk.ergo.{Amount, DhtData, ID, KioskBigInt}
import kiosk.explorer.Explorer
import kiosk.offchain.compiler.TxBuilder
import kiosk.offchain.parser.Parser
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{ConstantsBuilder, InputBox}
import org.sh.easyweb.Text
import org.sh.reflect.DataStructures.EasyMirrorSession
import scorex.crypto.hash.Blake2b256
import sigmastate.eval._
import sigmastate.interpreter.CryptoConstants
import special.sigma.GroupElement

class KioskWallet($ergoBox: KioskBoxCreator) extends EasyMirrorSession {
  private val explorer = new Explorer
  EasyWebEncoder
  val secretKey: BigInt = BigInt(Blake2b256($ergoBox.$ergoScript.$myEnv.$sessionSecret.getOrElse("none").getBytes("UTF-16")))
  private val defaultGenerator: GroupElement = CryptoConstants.dlogGroup.generator
  private val publicKey: GroupElement = defaultGenerator.exp(secretKey.bigInteger)
  val myAddress: String = {
    Client.usingClient { implicit ctx =>
      val contract = ctx.compileContract(
        ConstantsBuilder
          .create()
          .item(
            "gZ",
            publicKey
          )
          .build(),
        "proveDlog(gZ)"
      )
      val addressEncoder = new ErgoAddressEncoder(ctx.getNetworkType.networkPrefix)
      addressEncoder.fromProposition(contract.getErgoTree).get.toString
    }
  }

  def balance = {
    val boxes = explorer.getUnspentBoxes(myAddress)
    val nanoErgs: Long = boxes.map(_.value).sum
    val tokens: Map[ID, Amount] = boxes.flatMap(_.tokens).groupBy(_._1).map { case (k, v) => k -> v.map(_._2).sum }
    val ergs: String = nanoErgs / BigDecimal(1000000000) + " Ergs"
    val assets: Seq[String] = tokens.map { case (k, v) => k + " " + v }.toSeq
    ergs +: assets
  }

  private val randId = java.util.UUID.randomUUID().toString

  def send(toAddress: String, ergs: BigDecimal) = {
    val $INFO$ = "Using 0.001 Ergs as fee"
    val $ergs$ = "0.001"
    val nanoErgs = (ergs * BigDecimal(1000000000)).toBigInt().toLong
    val unspentBoxes: Seq[ergo.KioskBox] = explorer.getUnspentBoxes(myAddress).sortBy(-_.value)
    val boxName = randId
    $ergoBox.createBoxFromAddress(boxName, toAddress, Array(), Array(), Array(), nanoErgs)
    val inputs: Seq[String] = boxSelector(nanoErgs + defaultFee, unspentBoxes)
    val txJson = $ergoBox.createTx(
      inputBoxIds = inputs.toArray,
      dataInputBoxIds = Array(),
      outputBoxNames = Array(boxName),
      fee = defaultFee,
      changeAddress = myAddress,
      proveDlogSecrets = Array(secretKey.toString(10)),
      proveDhtDataNames = Array(),
      broadcast = true
    )
    $ergoBox.$deleteBox(boxName)
    txJson
  }

  private val defaultFee = 1000000L

  private def boxSelector(totalNanoErgsNeeded: Long, unspentBoxes: Seq[ergo.KioskBox]) = {
    var sum = 0L
    val unspentBoxSums: Seq[(Int, Long, Long)] = unspentBoxes.zipWithIndex.map {
      case (box, i) =>
        val sumBefore = sum
        sum = sumBefore + box.value
        (i + 1, sumBefore, sum)
    }
    val index: Int = unspentBoxSums
      .find { case (i, before, after) => before < totalNanoErgsNeeded && totalNanoErgsNeeded <= after }
      .getOrElse(throw new Exception(s"Insufficient funds. Short by ${totalNanoErgsNeeded - sum} nanoErgs"))
      ._1
    unspentBoxes.take(index).map(_.optBoxId.get)
  }

  private val compiler = new TxBuilder(explorer)

  def txBuilder(script: Text, additionalSecrets: Array[String], broadcast: Boolean) = {
    val $INFO$ =
      """This creates a transaction using the script specified in TxBuilder. 
If there any lacking Ergs or tokens in the inputs, the wallet will attempt to add its own unspent boxes. 
If some of the inputs need additional (proveDlog) secrets, they should be added to Env (as BigInts) and referenced in additionalSecrets"""

    val $broadcast$ = "false"

    val envMap = $ergoBox.$ergoScript.$myEnv.$envMap
    val additionalBigIntSecrets = additionalSecrets.map { additionalSecret =>
      if (envMap.contains(additionalSecret)) {
        $ergoBox.$ergoScript.$myEnv.$envMap(additionalSecret) match {
          case kioskBigInt: KioskBigInt => kioskBigInt.bigInt.toString(10)
          case any                      => throw new Exception(s"$additionalSecret must be of type BigInt. Found ${any.typeName}")
        }
      } else throw new Exception(s"Env does not contain (BigInt) variable $additionalSecret")
    }

    val compileResults = compiler.compile(Parser.parse(script.getText))
    val feeNanoErgs = compileResults.fee.getOrElse(defaultFee)
    val outputNanoErgs = compileResults.outputs.map(_.value).sum + feeNanoErgs
    val deficientNanoErgs = (outputNanoErgs - compileResults.inputNanoErgs).max(0)

    /* Currently we are not going to look for deficient tokens, just nanoErgs */
    val moreInputBoxIds = if (deficientNanoErgs > 0) {
      val myBoxes: Seq[ergo.KioskBox] = explorer.getUnspentBoxes(myAddress).filterNot(compileResults.inputBoxIds.contains).sortBy(-_.value)
      boxSelector(deficientNanoErgs, myBoxes)
    } else Nil
    val inputBoxIds = compileResults.inputBoxIds ++ moreInputBoxIds
    Client.usingClient { implicit ctx =>
      val inputBoxes: Array[InputBox] = ctx.getBoxesById(inputBoxIds: _*)
      val dataInputBoxes: Array[InputBox] = ctx.getBoxesById(compileResults.dataInputBoxIds: _*)
      $ergoBox.$createTx(
        inputBoxes = inputBoxes,
        dataInputs = dataInputBoxes,
        boxesToCreate = compileResults.outputs.toArray,
        fee = feeNanoErgs,
        changeAddress = myAddress,
        proveDlogSecrets = Array(secretKey.toString(10)) ++ additionalBigIntSecrets,
        dhtData = Array[DhtData](),
        broadcast = broadcast
      ).toJson(false)
    }
  }

  override def $setSession(sessionSecret: Option[String]): KioskWallet = new KioskWallet($ergoBox.$setSession(sessionSecret))
}
