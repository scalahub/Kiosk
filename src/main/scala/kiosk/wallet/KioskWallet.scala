package kiosk.wallet

import kiosk.appkit.Client
import kiosk.box.KioskBoxCreator
import kiosk.encoding.EasyWebEncoder
import kiosk.offchain.Compiler
import kiosk.{Reader, ergo}
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.ConstantsBuilder
import org.sh.easyweb.Text
import org.sh.reflect.DataStructures.EasyMirrorSession
import scorex.crypto.hash.Blake2b256
import sigmastate.eval._
import sigmastate.interpreter.CryptoConstants
import special.sigma.GroupElement

class KioskWallet($ergoBox: KioskBoxCreator) extends EasyMirrorSession {
  EasyWebEncoder
  private val secretKey: BigInt = BigInt(Blake2b256($ergoBox.$ergoScript.$myEnv.$sessionSecret.getOrElse("none").getBytes("UTF-16")))
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

  def balance = BigDecimal(Reader.getUnspentBoxes(myAddress).map(_.value).sum) / BigDecimal(1000000000) + " Ergs"

  def send(toAddress: String, ergs: BigDecimal) = {
    val $INFO$ = "Using 0.001 Ergs as fee"
    val $ergs$ = "0.001"
    val nanoErgs = (ergs * BigDecimal(1000000000)).toBigInt().toLong
    val feeNanoErgs = 1000000L
    val unspentBoxes: Seq[ergo.KioskBox] = Reader.getUnspentBoxes(myAddress).sortBy(-_.value)
    val boxName = scala.util.Random.nextString(100)
    $ergoBox.createBoxFromAddress(boxName, toAddress, Array(), Array(), Array(), nanoErgs)

    var sum = 0L
    val unspentBoxSums: Seq[(Int, Long, Long)] = unspentBoxes.zipWithIndex.map {
      case (box, i) =>
        val sumBefore = sum
        sum = sumBefore + box.value
        (i + 1, sumBefore, sum)
    }
    val needed = nanoErgs + feeNanoErgs
    val index: Int = unspentBoxSums.find { case (i, before, after) => before < needed && needed <= after }.getOrElse(throw new Exception("Insufficient funds"))._1
    val inputs: Seq[String] = unspentBoxes.take(index).map(_.optBoxId.get)
    val txJson = $ergoBox.createTx(inputs.toArray, Array(), Array(boxName), feeNanoErgs, toAddress, Array(secretKey.toString(10)), Array(), true)
    $ergoBox.$deleteBox(boxName)
    txJson
  }

  def eval(protocol: Text, broadcast: Boolean) = {
    val $broadcast$ = "false"
    Compiler.compile(protocol.getText)
  }
  override def $setSession(sessionSecret: Option[String]): KioskWallet = new KioskWallet($ergoBox.$setSession(sessionSecret))
}
