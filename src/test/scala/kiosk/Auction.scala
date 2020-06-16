package kiosk

import kiosk.encoding.ScalaErgoConverters
import kiosk.script.{ErgoScript, ErgoScriptEnv}
import org.ergoplatform.Pay2SAddress

object Auction extends App {
  val env = new ErgoScriptEnv

  // seller
  val alicePrivateKey = ECC.$randBigInt
  val alice = ScalaErgoConverters.stringToGroupElement(ECC.gX(alicePrivateKey))

  env.setGroupElement("alice", alice)

  val ergoScript = new ErgoScript(env) {}

  val source =
    """{
      |  val endTime = 5000 // auction end
      |  val minBidDelta = 1000000 // nano ergs
      |  val currBid = SELF.value
      |  val currBidder = SELF.R4[GroupElement].get
      |
      |  if (HEIGHT < endTime) {
      |     val newSelf = OUTPUTS(0) // new box created as a replica of current box
      |     val oldBidRefundBox = OUTPUTS(1)
      |     val newBid = newSelf.value
      |     val newBidder = newSelf.R4[GroupElement].get // just access it to ensure that it exists
      |
      |     newSelf.tokens(0)._1 == SELF.tokens(0)._1 &&
      |     newSelf.tokens(0)._2 == SELF.tokens(0)._2 &&
      |     newBid >= currBid + minBidDelta &&
      |     newSelf.value >= newBid &&
      |     oldBidRefundBox.propositionBytes == proveDlog(currBidder).propBytes &&
      |     oldBidRefundBox.value >= currBid
      |  } else {
      |     val winnerBox = OUTPUTS(0)
      |     val sellerBox = OUTPUTS(1)
      |
      |     winnerBox.tokens(0)._1 == SELF.tokens(0)._1 &&
      |     winnerBox.tokens(0)._2 == SELF.tokens(0)._2 &&
      |     winnerBox.propositionBytes == proveDlog(currBidder).propBytes &&
      |     sellerBox.value >= currBid &&
      |     sellerBox.propositionBytes == proveDlog(alice).propBytes
      |  }
      |
      |}""".stripMargin

  val ergoTree = ergoScript.$compile(source)

  import ErgoScript.$ergoAddressEncoder

  println("Auction address: "+Pay2SAddress(ergoTree))



}
