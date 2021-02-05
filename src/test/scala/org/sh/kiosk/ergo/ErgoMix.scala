package org.sh.kiosk.ergo

trait ErgoMix {

  val fullMixScriptSource =
    """{
      |  val g = groupGenerator
      |  val c1 = SELF.R4[GroupElement].get
      |  val c2 = SELF.R5[GroupElement].get
      |  val gX = SELF.R6[GroupElement].get
      |  proveDlog(c2) ||            // either c2 is g^y
      |  proveDHTuple(g, c1, gX, c2) // or c2 is u^y = g^xy
      |}""".stripMargin

  val halfMixScriptSource =
    """{
      |  val g = groupGenerator
      |  val gX = SELF.R4[GroupElement].get
      |
      |  val c1 = OUTPUTS(0).R4[GroupElement].get
      |  val c2 = OUTPUTS(0).R5[GroupElement].get
      |
      |  OUTPUTS(0).value == SELF.value &&
      |  OUTPUTS(1).value == SELF.value &&
      |  OUTPUTS(0).R6[GroupElement].get == gX &&
      |  OUTPUTS(1).R6[GroupElement].get == gX &&
      |  blake2b256(OUTPUTS(0).propositionBytes) == fullMixScriptHash &&
      |  blake2b256(OUTPUTS(1).propositionBytes) == fullMixScriptHash &&
      |  OUTPUTS(1).R4[GroupElement].get == c2 &&
      |  OUTPUTS(1).R5[GroupElement].get == c1 && {
      |    proveDHTuple(g, gX, c1, c2) ||
      |    proveDHTuple(g, gX, c2, c1)
      |  } && SELF.id == INPUTS(0).id
      |}""".stripMargin

}
