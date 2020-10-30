package kiosk.nonlazy

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import kiosk.script.{KioskScriptCreator, KioskScriptEnv}
import scorex.crypto.hash.Blake2b256
import kiosk.ergo._

object Split {

  val env = new KioskScriptEnv()
  val scriptCreator = new KioskScriptCreator(env)

  val splitScript =
    s"""{ 
       |  val validIn = SELF.id == INPUTS(0).id
       |  val ok = if (OUTPUTS(0).R4[Int].get == 1) {
       |    blake2b256(INPUTS(1).propositionBytes) == leftBranchBytesHash
       |  } else {
       |    blake2b256(INPUTS(1).propositionBytes) == rightBranchBytesHash
       |  }
       |  sigmaProp(ok && validIn)
       |}
       |""".stripMargin

  val leftScript =
    s"""{
       |  sigmaProp(CONTEXT.dataInputs(0).R4[Long].get <= INPUTS(0).value)
       |}""".stripMargin

  val rightScript =
    s"""{
       |  sigmaProp(CONTEXT.dataInputs(0).R4[Coll[Byte]].get != INPUTS(0).propositionBytes)
       |}""".stripMargin

  val leftErgoTree = scriptCreator.$compile(leftScript)
  val rightErgoTree = scriptCreator.$compile(rightScript)

  env.setCollByte("leftBranchBytesHash", Blake2b256(leftErgoTree.bytes))
  env.setCollByte("rightBranchBytesHash", Blake2b256(rightErgoTree.bytes))

  val splitErgoTree = scriptCreator.$compile(splitScript)

  val splitAddress = getStringFromAddress(getAddressFromErgoTree(splitErgoTree))
  val leftAddress = getStringFromAddress(getAddressFromErgoTree(leftErgoTree))
  val rightAddress = getStringFromAddress(getAddressFromErgoTree(rightErgoTree))

  def main(args: Array[String]): Unit = {
    println(splitAddress)
    assert(
      splitAddress == "2PELCgrp5nNgVKMAEky7GjT8VxL7Xsc7z7ocVcEW4e1zhKSrzwVSavg3C4AbbN2xM4vRSFQv4EVDarTChJnwg6wwEURFj5VjMv7nVpAm8jaahzZZoJJqJRHaEu2zteSzMXsYBHGsQDD5m5JPsp3hkZ8qzXcgBd29TzTfEqh9i8FnFe3X")
  }
}
