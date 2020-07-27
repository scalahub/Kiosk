package org.sh.kiosk.ergo

import kiosk.ergo._
import kiosk.script.{KioskScriptCreator, KioskScriptEnv}

/*
This is variant of the OraclePool using dynamic (or variable) epoch times. The difference is that contracts are much more simpler but we lose the ability to do stake slashing
 */
object OraclePool extends App {
  val env = new KioskScriptEnv()
  val scriptCreator = new KioskScriptCreator(env)

  val oracleBoxSource =
    """
      |{
      |  val groupElement = SELF.R5[GroupElement].get
      |
      |  OUTPUTS(0).tokens == SELF.tokens &&
      |  OUTPUTS(0).propositionBytes == SELF.propositionBytes &&
      |  OUTPUTS(0).R5[GroupElement].get == groupElement &&
      |  INPUTS.filter({(b:Box) => b.propositionBytes == SELF.propositionBytes}).size == 1 && proveDlog(groupElement)
      |}
      |""".stripMargin

  val oracleBoxScript = scriptCreator.$compile(oracleBoxSource)
  println("Oracle box script: "+oracleBoxScript.bytes.encodeHex)

  val oracleBoxScriptBytes = oracleBoxScript.bytes

  val managementBoxSource =
    """
      |{
      |  val members = SELF.R4[Coll[GroupElement]].get
      |  val publicKeys = members.map{(g:GroupElement) => proveDlog(g)}
      |  val threshold = SELF.R5[Int].get
      |  val o = OUTPUTS(0) // copy of self
      |  val p = OUTPUTS(1) // purchase box
      |  val isTokenIssue =
      |    o.propositionBytes == SELF.propositionBytes &&
      |    o.tokens(0)._1 == SELF.tokens(0)._1 &&
      |    o.tokens(0)._2 == (SELF.tokens(0)._2 + 1) &&
      |    o.R4[Coll[GroupElement]].get == members &&
      |    o.R5[Int].get == threshold &&
      |    o.value == SELF.value &&
      |    p.tokens(0)._1 == SELF.tokens(0)._1 &&
      |    p.propositionBytes == oracleBoxScriptBytes
      |
      |  atLeast(threshold, publicKeys) && isTokenIssue
      |}
      |""".stripMargin

  env.setCollByte("oracleBoxScriptBytes", oracleBoxScriptBytes)
  val managementScript = scriptCreator.$compile(managementBoxSource)
  println("Management box script: "+managementScript.bytes.encodeHex)

  val oracleTokenId: Array[Byte] = "dummy_oracle_token_id".getBytes()
  val epoch = 30 // 1 hour
  val collectorRewardPerData = 15000000 // Nano ergs. One reward per data point included in data inputs. The more the data points, the more the reward
  val oracleReward = 150000000 // Nano ergs. One reward per data point to be paid to oracle
  val minNumOracles = 1 // how many min needed
  val confDelay = 4 // how many min needed
  val minOracleBoxValue = 1000000000 // min nano ergs in oracle box

  val poolBoxSource =
    """
      |{
      |  val oracleBoxes = CONTEXT.dataInputs.filter{(b:Box) =>
      |    b.creationInfo._1 >= HEIGHT - epoch &&
      |    b.tokens(0)._1 == oracleTokenId
      |  }
      |
      |  val sum = oracleBoxes.fold(0L, { (t:Long, b: Box) => t + b.R4[Long].get })
      |
      |  val average = if (oracleBoxes.size > 0) sum / oracleBoxes.size else 0L
      |
      |  val oracleRewardOutputs = oracleBoxes.fold((1, true), { (t:(Int, Boolean), b:Box) =>
      |    (t._1 + 1, t._2 && OUTPUTS(t._1).propositionBytes == proveDlog(b.R5[GroupElement].get).propBytes && OUTPUTS(t._1).value == oracleReward)
      |  })
      |
      |  OUTPUTS(0).propositionBytes == SELF.propositionBytes &&
      |  OUTPUTS(0).tokens == SELF.tokens &&
      |  OUTPUTS(0).value >= minOracleBoxValue && (
      |    ( // collection
      |      oracleBoxes.size >= minNumOracles &&
      |      OUTPUTS(0).R4[Long].get == average &&
      |      OUTPUTS(0).creationInfo._1 >= HEIGHT - confDelay &&
      |      OUTPUTS(0).creationInfo._1 >= SELF.creationInfo._1 + epoch &&
      |      OUTPUTS(0).value >= SELF.value - oracleBoxes.size * (collectorRewardPerData + oracleReward) &&
      |      oracleRewardOutputs._2
      |    ) || ( // top-up
      |      OUTPUTS(0).creationInfo._1 == SELF.creationInfo._1 &&
      |      OUTPUTS(0).R4[Long].get == SELF.R4[Long].get &&
      |      OUTPUTS(0).value > SELF.value
      |    )
      |  )
      |}
      |""".stripMargin

  env.setCollByte("oracleTokenId", oracleTokenId)
  env.setLong("collectorRewardPerData", collectorRewardPerData)
  env.setLong("minOracleBoxValue", minOracleBoxValue)
  env.setLong("oracleReward", oracleReward)
  env.setInt("epoch", epoch)
  env.setInt("minNumOracles", minNumOracles)
  env.setInt("confDelay", confDelay)

  val poolBoxScript = scriptCreator.$compile(poolBoxSource)
  println("Pool box script: "+poolBoxScript.bytes.encodeHex)

}
