package org.sh.kiosk.ergo

import org.ergoplatform.Pay2SAddress
import org.sh.kiosk.ergo.util.ErgoScriptUtil.{getRandomBigInt, hexToGroupElement, serialize}
import scorex.crypto.hash.Blake2b256
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer

object Auction extends App {
  val env = new Env

  // seller
  val alicePrivateKey = getRandomBigInt
  val alice = hexToGroupElement(ECC.gExp(alicePrivateKey))

  env.setGroupElement("alice", alice)

  val ergoScript = new ErgoScript(env) {}

  val source =
    """{
      |  val endTime = 5000 // auction end
      |  val minBidDelta = 1000000 // nano ergs
      |  val currBid = SELF.R4[Long].get
      |  val currBidder = SELF.R5[GroupElement].get
      |
      |  if (HEIGHT < endTime) {
      |     val newSelf = OUTPUTS(0) // new box created as a replica of current box
      |     val oldBidRefundBox = OUTPUTS(1)
      |     val newBid = newSelf.R4[Long].get
      |     val newBidder = newSelf.R5[GroupElement].get // just access it to ensure that it exists
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

  import ergoScript.$ergoAddressEncoder

  println("Auction address: "+Pay2SAddress(ergoTree))



}
