package kiosk.timestamp.v1

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import kiosk.script.{KioskScriptCreator, KioskScriptEnv}
import scorex.crypto.hash.Blake2b256
import kiosk.ergo._

object Timestamp {

  val env = new KioskScriptEnv()
  val scriptCreator = new KioskScriptCreator(env)

  val buffer: Int = 5 // blocks
  val minStorageRent: Long = 1500000L

  val timestampScript = "sigmaProp(false)"
  val timestampErgoTree = scriptCreator.$compile(timestampScript)

  env.setCollByte("timestampScriptBytes", timestampErgoTree.bytes)

  val emissionScript =
    s"""{ 
       |  
       |  val out = OUTPUTS(0)
       |  val box = CONTEXT.dataInputs(0)
       |  val timestampBox = OUTPUTS(1)
       |  
       |  val inTokenId = SELF.tokens(0)._1
       |  val inTokens = SELF.tokens(0)._2
       |  val outTokenId = out.tokens(0)._1
       |  val outTokens = out.tokens(0)._2
       |  
       |  val validIn = SELF.id == INPUTS(0).id
       |   
       |  val validOut = out.propositionBytes == SELF.propositionBytes && out.value >= SELF.value
       |     
       |  val validTokens = outTokenId == inTokenId && inTokens == (outTokens + 1)
       |  
       |  val validTimestamp = timestampBox.R4[Coll[Byte]].get == box.id && 
       |                       timestampBox.R5[Int].get >= (HEIGHT - $buffer) && 
       |                       timestampBox.propositionBytes == timestampScriptBytes && 
       |                       timestampBox.tokens(0)._1 == inTokenId
       |    
       |  sigmaProp(
       |    validIn && validOut && validTokens && validTimestamp
       |  )
       |}
       |""".stripMargin

  val emissionErgoTree = scriptCreator.$compile(emissionScript)
  val emissionScriptHash = Blake2b256(emissionErgoTree.bytes)

  env.setCollByte("emissionScriptHash", emissionScriptHash)
  val masterScript =
    s"""{
       |  val out = OUTPUTS(0)
       |  val emissionBox = OUTPUTS(1)
       |  
       |  val validIn = SELF.id == INPUTS(0).id
       |  
       |  val validOut = out.propositionBytes == SELF.propositionBytes && 
       |                 out.tokens(0)._1 == SELF.tokens(0)._1 && 
       |                 SELF.tokens(0)._2 == (out.tokens(0)._2 + 1000) && 
       |                 out.value == SELF.value
       |                 
       |  val validEmissionBox = blake2b256(emissionBox.propositionBytes) == emissionScriptHash && 
       |                         emissionBox.tokens(0)._1 == SELF.tokens(0)._1 && 
       |                         emissionBox.tokens(0)._2 == 1000 &&
       |                         emissionBox.value >= $minStorageRent
       |                           
       |  sigmaProp(validIn && validOut && validEmissionBox)
       |}
       |""".stripMargin
  val emissionAddress = getStringFromAddress(getAddressFromErgoTree(emissionErgoTree))

  val masterErgoTree = scriptCreator.$compile(masterScript)
  val masterAddress = getStringFromAddress(getAddressFromErgoTree(masterErgoTree))

  def main(args: Array[String]): Unit = {
    println(emissionAddress)
    println(masterAddress)
    println("timestamp ErgoTree (hex) " + timestampErgoTree.bytes.encodeHex)
    assert(
      emissionAddress == "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi")
    assert(
      masterAddress == "2vTQnMx5uFfFfJjL6ucuprpWSUeXHAqbyPLkW46DfMgw7ENGFbGBVPHJPVXwJWg5e1DdqPv28syDEJQGQy5vss2Wvh6Srrd98fSSTVfkEb5VcehCqhoGD8826imCkAfC2mDhGcTuYKcFvy4JrC8GoAbx6NZomHZAmESCL8QyQ2utraCF7TebrZGudEDehwho4AMQkq9oDkaVdyQ2NNuYQ8NwtQcBrfCZRFSGGeitPmnoCQgK8vQDxBifiQcW1avYexPYdb9CXHGT8EtKaRj5JXcqcuwwsXp5GXfG")
  }
}
