package kiosk.nonlazy

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import kiosk.script.{KioskScriptCreator, KioskScriptEnv}
import scorex.crypto.hash.Blake2b256
import kiosk.ergo._

object Branch {

  val env = new KioskScriptEnv()
  val scriptCreator = new KioskScriptCreator(env)

  val branchScript =
    s"""{ 
       |  val ok = if (OUTPUTS(0).R4[Int].get == 1) {
       |    CONTEXT.dataInputs(0).R4[Long].get <= SELF.value
       |  } else { // assume Coll[Byte]
       |    CONTEXT.dataInputs(0).R4[Coll[Byte]].get != SELF.propositionBytes
       |  }
       |  sigmaProp(ok)
       |}
       |""".stripMargin

  val branchErgoTree = scriptCreator.$compile(branchScript)

  val branchBoxAddress = getStringFromAddress(getAddressFromErgoTree(branchErgoTree))

  def main(args: Array[String]): Unit = {
    println(branchBoxAddress)
    assert(branchBoxAddress == "88dwYDNXcCq9UyA7VBcSdqJRgooKVqS8ixprCknxcm2sba4jbhQYGphjutEebtr3ZeC4tmT9oEWKS2Bq")
  }
}
