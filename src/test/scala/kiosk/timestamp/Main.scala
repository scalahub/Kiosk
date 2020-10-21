package kiosk.timestamp

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import kiosk.ergo._
import kiosk.script.{KioskScriptCreator, KioskScriptEnv}
import scorex.crypto.hash.Blake2b256

object Main extends App {

  val env = new KioskScriptEnv()
  val scriptCreator = new KioskScriptCreator(env)

  val buffer: Int = 5 // blocks
  val minStorageRent: Long = 10000000L
  val fee: Long = 10000000L

  val aliceGBytes = "0290d9bbac88042a69660b263b4afc29a2084a0ffce4665de89211846d42bb30e4".decodeHex
  env.setCollByte("aliceGBytes", aliceGBytes)

  val emissionScript =
    s"""{ 
       |  val alice = proveDlog(decodePoint(aliceGBytes))
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
       |  val validOut = out.propositionBytes == SELF.propositionBytes  
       |  
       |  val mandatory = validIn && validOut 
       |  
       |  val validTimestamp = timestampBox.R4[Coll[Byte]].get == box.id && 
       |                       timestampBox.R5[Int].get >= (HEIGHT - $buffer) && 
       |                       timestampBox.propositionBytes == sigmaProp(false).propBytes && 
       |                       timestampBox.tokens(0)._1 == inTokenId
       |    
       |  val aliceSpends = alice && mandatory && 
       |                    out.tokens == SELF.tokens && 
       |                    out.value >= $minStorageRent
       |  
       |  val emitTimestamp = mandatory && out.value >= SELF.value + $fee && 
       |                      outTokenId == inTokenId && 
       |                      inTokens == outTokens + 1 && validTimestamp
       |  
       |  aliceSpends || emitTimestamp
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
       |                         emissionBox.tokens(0)._2 == 1000
       |                           
       |  sigmaProp(validOut && validEmissionBox)
       |}
       |""".stripMargin
  val emissionAddress = getStringFromAddress(getAddressFromErgoTree(emissionErgoTree))
  println(emissionAddress)

  val masterErgoTree = scriptCreator.$compile(masterScript)
  val masterAddress = getStringFromAddress(getAddressFromErgoTree(masterErgoTree))
  println(masterAddress)
}
