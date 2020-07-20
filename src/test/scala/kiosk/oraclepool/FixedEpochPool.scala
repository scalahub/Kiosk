package kiosk.oraclepool

import kiosk.ECC
import kiosk.ergo._
import kiosk.script.{KioskScriptCreator, KioskScriptEnv}

object FixedEpochPool extends App {
  val env = new KioskScriptEnv()
  val scriptCreator = new KioskScriptCreator(env)

  /*
        <--------------epochPeriod-----------><---------prepPeriod-------->
 ... ------------------------------------------------------------------------------
        ^                                     ^                           ^
        |                                     |                           |
        epoch start                           epoch end                   epoch preparation end
                                              (epoch preparation start)   (next epoch start)


   */

  // constants
  val livePeriod = 50 // blocks
  val prepPeriod = 10 // blocks
  val epochPeriod = livePeriod + prepPeriod
  val buffer = 4 // blocks

  val oracleTokenId: Array[Byte] = "dummy_oracle_token_id".getBytes()

  val oracleReward = 150000000 // Nano ergs. One reward per data point to be paid to oracle
  val minPoolBoxValue = 1000000000 // how much min must exist in oracle pool box

  val oracle1PrvKey = ECC.$randBigInt
  val oracle1PubKey = ECC.$gX(oracle1PrvKey)

  val oracle2PrvKey = ECC.$randBigInt
  val oracle2PubKey = ECC.$gX(oracle2PrvKey)

  val oracle3PrvKey = ECC.$randBigInt
  val oracle3PubKey = ECC.$gX(oracle3PrvKey)

  val oracle4PrvKey = ECC.$randBigInt
  val oracle4PubKey = ECC.$gX(oracle4PrvKey)

  val oraclePubKeys = Array(oracle1PubKey, oracle2PubKey, oracle3PubKey, oracle4PubKey)

  env.setCollGroupElement("oraclePubKeys", oraclePubKeys)

  val activeEpochScript =
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
       |  val prepEpochScriptBytes = SELF.R6[Coll[Byte]].get
       |
       |  OUTPUTS(0).propositionBytes == prepEpochScriptBytes &&
       |  oracleBoxes.size > 0 && anyOracle &&
       |  OUTPUTS(0).tokens == SELF.tokens &&
       |  OUTPUTS(0).R4[Long].get == average &&
       |  OUTPUTS(0).R5[Int].get == SELF.R5[Int].get + $epochPeriod &&
       |  OUTPUTS(0).value >= $minPoolBoxValue &&
       |  OUTPUTS(0).R4[Long].get == average &&
       |  OUTPUTS(0).value >= SELF.value - (oracleBoxes.size + 1) * oracleReward &&
       |  oracleRewardOutputs._2
       |}
       |""".stripMargin

  val prepEpochScript =
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
       |  val isActiveEpochOutput =  OUTPUTS(0).R6[Coll[Byte]].get == SELF.propositionBytes &&
       |                             OUTPUTS(0).propositionBytes == activeEpochScriptBytes
       |  ( // start next epoch
       |    epochNotOver && canStartEpoch && enoughFunds &&
       |    OUTPUTS(0).R4[Long].get == SELF.R4[Long].get &&
       |    OUTPUTS(0).R5[Int].get == SELF.R5[Int].get &&
       |    OUTPUTS(0).tokens == SELF.tokens &&
       |    OUTPUTS(0).value >= SELF.value &&
       |    isActiveEpochOutput
       |  ) || ( // create new epoch
       |    epochOver && enoughFunds &&
       |    OUTPUTS(0).R4[Long].get == SELF.R4[Long].get &&
       |    OUTPUTS(0).R5[Int].get >= minNewEpochHeight &&
       |    OUTPUTS(0).R5[Int].get <= maxNewEpochHeight &&
       |    OUTPUTS(0).tokens == SELF.tokens &&
       |    OUTPUTS(0).value >= SELF.value &&
       |    isActiveEpochOutput
       |  ) || ( // collect funds
       |    OUTPUTS(0).R4[Long].get == SELF.R4[Long].get &&
       |    OUTPUTS(0).R5[Int].get == SELF.R5[Int].get &&
       |    OUTPUTS(0).propositionBytes == SELF.propositionBytes &&
       |    OUTPUTS(0).tokens == SELF.tokens &&
       |    OUTPUTS(0).value > SELF.value
       |  )
       |}
       |""".stripMargin


  env.setCollByte("oracleTokenId", oracleTokenId)
  env.setLong("minPoolBoxValue", minPoolBoxValue)
  env.setLong("oracleReward", oracleReward)

  val activeEpochErgoTree = scriptCreator.$compile(activeEpochScript)
  println("Active Epoch script: "+activeEpochErgoTree.bytes.encodeHex)

  env.setCollByte("activeEpochScriptBytes", activeEpochErgoTree.bytes)

  val prepEpochErgoTree = scriptCreator.$compile(prepEpochScript)
  println("Prep Epoch script: "+prepEpochErgoTree.bytes.encodeHex)

}
