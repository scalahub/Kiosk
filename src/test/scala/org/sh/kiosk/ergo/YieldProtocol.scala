package org.sh.kiosk.ergo

import kiosk.ECC
import kiosk.script.{KioskScriptCreator, KioskScriptEnv}
import scorex.crypto.hash.Blake2b256
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer

/*
  Using the idea from http://research.paradigm.xyz/Yield.pdf

  This version uses a standalone token (no management contract)
 */
object YieldProtocol extends App {
  /*
  Alice is bond issuer; she creates a box with a large number of
  tokens (as large as possible)

   */

  val liquidatedBoxSrc =
    """
      |{
      |  val bondOwner = proveDlog(alice)
      |
      |  val fixedRate = SELF.R4[Long].get // nanoErgs per usdCent at time of liquidation
      |  val maxRedeemTime = SELF.R5[Int].get
      |
      |  val tokenID = SELF.tokens(0)._1 // tokenID that maps to bonds
      |  val tokenNum = SELF.tokens(0)._2 // how many bond tokens left
      |
      |  val newBox = OUTPUTS(0)
      |  val newBoxTokenID = newBox.tokens(0)._1
      |  val newBoxTokenNum = newBox.tokens(0)._2 // how many bond tokens left
      |  val bondDiff = newBoxTokenNum - tokenNum
      |  val ergsDiff = SELF.value - newBox.value
      |
      |  val validNewBox = newBox.propositionBytes == SELF.propositionBytes &&
      |                    newBoxTokenID == tokenID &&
      |                    bondDiff >= 10000 && // at least 100 USD difference (prevent micro tx)
      |                    ergsDiff <= bondDiff * fixedRate &&
      |                    newBox.R4[Long].get == fixedRate &&
      |                    newBox.R5[Int].get == maxRedeemTime
      |
      |  (bondOwner && (HEIGHT > maxRedeemTime)) || validNewBox
      |}
      |""".stripMargin

  val bondBoxSource =
    """{
      |  val numBonds = SELF.R4[Long].get // how many bonds issued (one bond = 1 USD cent)
      |  val tokenID = SELF.tokens(0)._1 // tokenID that maps to bonds
      |  val tokenNum = SELF.tokens(0)._2 // how many bond tokens left
      |
      |  val newBox = OUTPUTS(0)
      |  val newBoxTokenID = newBox.tokens(0)._1
      |  val newBoxTokenNum = newBox.tokens(0)._2 // how many bond tokens left
      |  val validNewBoxToken = tokenID == newBoxTokenID
      |
      |  val rateBox = CONTEXT.dataInputs(0)
      |  val rate = rateBox.R4[Long].get // nanoErgs per usdCent
      |  val validRateBox = rateBox.tokens(0)._1 == rateTokenID
      |
      |  val lockedErgs = SELF.value // nanoErgs
      |  val neededErgs = numBonds * rate
      |
      |  val insufficientErgs = lockedErgs * 10 >= neededErgs * 11  // at least 10 percent margin
      |
      |  if (HEIGHT > endHeight || insufficientErgs) {
      |     // bond ended or margin call
      |     blake2b256(newBox.propositionBytes) == liquidatedBoxScriptHash &&
      |     validNewBoxToken && newBoxTokenNum == tokenNum &&
      |     newBox.R4[Long].get == rate &&
      |     newBox.R5[Int].get >= HEIGHT + withdrawDeadline
      |  } else {
      |     // purchase bonds
      |     val numTokensReduced = tokenNum - newBoxTokenNum
      |     val numNewBonds = newBox.R4[Long].get
      |     val numBondsIncreased = numNewBonds - numBonds
      |     val ergsIncreased = newBox.value - SELF.value
      |
      |     val validErgsIncrease = ergsIncreased >= numBondsIncreased * rate
      |
      |     newBox.propositionBytes == SELF.propositionBytes &&
      |     numBondsIncreased >= minBondsToPurchase &&
      |     numBondsIncreased == numTokensReduced &&
      |     validErgsIncrease &&
      |     numNewBonds <= maxBonds
      |  }
      |}""".stripMargin

  val rateOracleTokenID:Array[Byte] = Blake2b256("rate").toArray // To use the correct id in real world
  // issuer
  val alicePrivateKey = ECC.$randBigInt
  val alice = ECC.$gX(alicePrivateKey)

  val env = new KioskScriptEnv
  env.setCollByte("rateTokenID", rateOracleTokenID)
  env.setGroupElement("alice", alice)

  val compiler1 = new KioskScriptCreator(env)
  val liquidatedBoxErgoTree = compiler1.$compile(liquidatedBoxSrc)

  val liquidatedBoxScriptBytes = DefaultSerializer.serializeErgoTree(liquidatedBoxErgoTree)
  val liquidatedBoxScriptHash = scorex.crypto.hash.Blake2b256(liquidatedBoxScriptBytes)

  env.setCollByte("liquidatedBoxScriptHash", liquidatedBoxScriptHash)

  val maxBonds = 100000000L
  val minBondsToPurchase = 1000L
  val endHeight = 100000 // height at which bond ends
  val withdrawDeadline = 100000 // minimum delay given to withdraw

  env.setLong("maxBonds", maxBonds)
  env.setLong("minBondsToPurchase", minBondsToPurchase)
  env.setInt("endHeight", endHeight)
  env.setLong("withdrawDeadline", withdrawDeadline)

  val compiler2 = new KioskScriptCreator(env) {}
  compiler2.$compile(bondBoxSource)
  println("done")
}
