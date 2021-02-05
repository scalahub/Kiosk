package kiosk.nonlazy

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import scorex.crypto.hash.Blake2b256
import kiosk.ergo._
import kiosk.script.ScriptUtil

object Split {

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

  val leftErgoTree = ScriptUtil.compile(Map(), leftScript)
  val rightErgoTree = ScriptUtil.compile(Map(), rightScript)

  val splitErgoTree = ScriptUtil.compile(
    Map(
      "leftBranchBytesHash" -> KioskCollByte(Blake2b256(leftErgoTree.bytes)),
      "rightBranchBytesHash" -> KioskCollByte(Blake2b256(rightErgoTree.bytes))
    ),
    splitScript
  )

  val splitAddress = getStringFromAddress(getAddressFromErgoTree(splitErgoTree))
  val leftAddress = getStringFromAddress(getAddressFromErgoTree(leftErgoTree))
  val rightAddress = getStringFromAddress(getAddressFromErgoTree(rightErgoTree))

}
