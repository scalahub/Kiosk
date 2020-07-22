package kiosk.oraclepool

import kiosk.script.{KioskScriptCreator, KioskScriptEnv}

abstract class FixedEpochPool extends App {
  val env = new KioskScriptEnv()
  val scriptCreator = new KioskScriptCreator(env)

  /*
        <--------------livePeriod------------><---------prepPeriod-------->
 ... ------------------------------------------------------------------------------
        ^                                     ^                           ^
        |                                     |                           |
        epoch start                           epoch end                   epoch preparation end
                                              (epoch preparation start)   (next epoch start)


   */

  // constants
  val livePeriod:Int // blocks
  val prepPeriod:Int // blocks
  lazy val epochPeriod = livePeriod + prepPeriod
  val buffer:Int // blocks

  val oracleTokenId: Array[Byte]
  val poolTokenId: Array[Byte]

  val oracleReward:Long // Nano ergs. One reward per data point to be paid to oracle
  val minPoolBoxValue:Long // how much min must exist in oracle pool box

  type GroupElementHex = String
  val oraclePubKeys: Array[GroupElementHex]

  env.setCollGroupElement("oraclePubKeys", oraclePubKeys)
  env.setCollByte("oracleTokenId", oracleTokenId)
  env.setCollByte("poolTokenId", poolTokenId)
  env.setLong("minPoolBoxValue", minPoolBoxValue)
  env.setLong("oracleReward", oracleReward)

  val liveEpochScript =
    s"""{ // This box:
       |  // R4: The latest finalized datapoint (from the previous epoch)
       |  // R5: Block height that the current epoch will finish on
       |  // R6: Address of the "Epoch Preparation" stage contract.
       |
       |  // Oracle box:
       |  // R4: Public key (group element)
       |  // R5: Epoch box Id (this box's Id)
       |  // R6: Data point
       |
       |  val oracleBoxes = CONTEXT.dataInputs.filter{(b:Box) =>
       |    b.R5[Coll[Byte]].get == SELF.id &&
       |    b.tokens(0)._1 == oracleTokenId
       |  }
       |
       |  val proveDlogs = oraclePubKeys.map{(grp:GroupElement) => proveDlog(grp)}
       |
       |  val anyOracle = anyOf(proveDlogs)
       |
       |  val sum = oracleBoxes.fold(0L, { (t:Long, b: Box) => t + b.R6[Long].get })
       |
       |  val average = sum / oracleBoxes.size // do we need to check for division by zero here?
       |
       |  val oracleRewardOutputs = oracleBoxes.fold((1, true), { (t:(Int, Boolean), b:Box) =>
       |    (t._1 + 1, t._2 &&
       |               OUTPUTS(t._1).propositionBytes == proveDlog(b.R4[GroupElement].get).propBytes &&
       |               OUTPUTS(t._1).value >= $oracleReward)
       |  })
       |
       |  val epochPrepScriptBytes = SELF.R6[Coll[Byte]].get
       |
       |  sigmaProp(
       |    OUTPUTS(0).propositionBytes == epochPrepScriptBytes &&
       |    oracleBoxes.size > 0 &&
       |    OUTPUTS(0).tokens == SELF.tokens &&
       |    OUTPUTS(0).R4[Long].get == average &&
       |    OUTPUTS(0).R5[Int].get == SELF.R5[Int].get + $epochPeriod &&
       |    OUTPUTS(0).value >= $minPoolBoxValue &&
       |    OUTPUTS(0).R4[Long].get == average &&
       |    OUTPUTS(0).value >= SELF.value - (oracleBoxes.size + 1) * oracleReward &&
       |    oracleRewardOutputs._2 && anyOracle
       |  )
       |}
       |""".stripMargin

  val epochPrepScript =
    s"""
       |{
       |  // This box:
       |  // R4: The finalized data point from collection
       |  // R5: Height the epoch will end
       |
       |  val canStartEpoch = HEIGHT > SELF.R5[Int].get - $livePeriod + $prepPeriod
       |  val epochNotOver = HEIGHT < SELF.R5[Int].get
       |  val epochOver = HEIGHT >= SELF.R5[Int].get
       |  val enoughFunds = SELF.value >= (${oraclePubKeys.length} + 1) * $oracleReward + $minPoolBoxValue
       |
       |  val maxNewEpochHeight = HEIGHT + $epochPeriod + $buffer
       |  val minNewEpochHeight = HEIGHT + $epochPeriod
       |
       |  val isliveEpochOutput =  OUTPUTS(0).R6[Coll[Byte]].get == SELF.propositionBytes &&
       |                             OUTPUTS(0).propositionBytes == liveEpochScriptBytes
       |  sigmaProp( // start next epoch
       |    epochNotOver && canStartEpoch && enoughFunds &&
       |    OUTPUTS(0).R4[Long].get == SELF.R4[Long].get &&
       |    OUTPUTS(0).R5[Int].get == SELF.R5[Int].get &&
       |    OUTPUTS(0).tokens == SELF.tokens &&
       |    OUTPUTS(0).value >= SELF.value &&
       |    isliveEpochOutput
       |  ) || sigmaProp( // create new epoch
       |    epochOver && enoughFunds &&
       |    OUTPUTS(0).R4[Long].get == SELF.R4[Long].get &&
       |    OUTPUTS(0).R5[Int].get >= minNewEpochHeight &&
       |    OUTPUTS(0).R5[Int].get <= maxNewEpochHeight &&
       |    OUTPUTS(0).tokens == SELF.tokens &&
       |    OUTPUTS(0).value >= SELF.value &&
       |    isliveEpochOutput
       |  ) || sigmaProp( // collect funds
       |    OUTPUTS(0).R4[Long].get == SELF.R4[Long].get &&
       |    OUTPUTS(0).R5[Int].get == SELF.R5[Int].get &&
       |    OUTPUTS(0).propositionBytes == SELF.propositionBytes &&
       |    OUTPUTS(0).tokens == SELF.tokens &&
       |    OUTPUTS(0).value > SELF.value
       |  )
       |}
       |""".stripMargin

  val dataPointScript =
    s"""
       |{
       |  // This box:
       |  // R4: The address of the oracle (never allowed to change after bootstrap).
       |  // R5: The box id of the latest Live Epoch box.
       |  // R6: The oracle's datapoint.
       |
       |  val pubKey = SELF.R4[GroupElement].get
       |
       |  val liveEpochBox = CONTEXT.dataInputs(0)
       |
       |  val validLiveEpochBox = liveEpochBox.tokens(0)._1 == oracleTokenId &&
       |                          liveEpochBox.propositionBytes == liveEpochScriptBytes
       |
       |  sigmaProp(
       |    OUTPUTS(0).R4[GroupElement].get == pubKey &&
       |    OUTPUTS(0).R5[Coll[Byte]].get == liveEpochBox.id &&
       |    OUTPUTS(0).R6[Long].get > 0 &&
       |    OUTPUTS(0).propositionBytes == SELF.propositionBytes &&
       |    OUTPUTS(0).tokens == SELF.tokens &&
       |    validLiveEpochBox
       |  ) && proveDlog(pubKey)
       |}
       |""".stripMargin

  val poolDepositScript =
    s"""
       |{
       |  val allFundingBoxes = INPUTS.filter{(b:Box) =>
       |    b.propositionBytes == SELF.propositionBytes
       |  }
       |
       |  val totalFunds = allFundingBoxes.fold(0L, { (t:Long, b: Box) => t + b.value })
       |
       |  sigmaProp(
       |    INPUTS(0).propositionBytes == epochPrepScriptBytes &&
       |    OUTPUTS(0).propositionBytes == epochPrepScriptBytes &&
       |    OUTPUTS(0).value >= INPUTS(0).value + totalFunds &&
       |    OUTPUTS(0).tokens(0)._1 == poolTokenId
       |  )
       |}
       |""".stripMargin

  val liveEpochErgoTree = scriptCreator.$compile(liveEpochScript)
  env.setCollByte("liveEpochScriptBytes", liveEpochErgoTree.bytes)
  val epochPrepErgoTree = scriptCreator.$compile(epochPrepScript)
  val dataPointErgoTree = scriptCreator.$compile(dataPointScript)
  env.setCollByte("epochPrepScriptBytes", epochPrepErgoTree.bytes)
  val poolDepositErgoTree = scriptCreator.$compile(poolDepositScript)

  println(s"Live Epoch script length       : ${liveEpochErgoTree.bytes.length}")
  println(s"Live Epoch script complexity   : ${liveEpochErgoTree.complexity}")
  println(s"Epoch prep script length       : ${epochPrepErgoTree.bytes.length}")
  println(s"Epoch prep script complexity   : ${epochPrepErgoTree.complexity}")
  println(s"DataPoint script length        : ${dataPointErgoTree.bytes.length}")
  println(s"DataPoint script complexity    : ${dataPointErgoTree.complexity}")
  println(s"PoolDeposit script length      : ${poolDepositErgoTree.bytes.length}")
  println(s"PoolDeposit script complexity  : ${poolDepositErgoTree.complexity}")

}
