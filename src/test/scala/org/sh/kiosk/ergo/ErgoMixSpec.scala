package org.sh.kiosk.ergo

import kiosk.ergo._
import kiosk.script.ScriptUtil
import org.ergoplatform.Pay2SAddress
import org.scalatest.{Matchers, WordSpec}
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer

import scala.collection.mutable.{Map => MMap}

class ErgoMixSpec extends WordSpec with Matchers with ErgoMix {

  import kiosk.script.ScriptUtil._

  "ErgoMix script" should {
    "compile correctly" in {
      val env = MMap[String, KioskType[_]]()
      val fullMixErgoTree = ScriptUtil.compile(env.toMap, fullMixScriptSource)
      val fullMixScriptBytes = DefaultSerializer.serializeErgoTree(fullMixErgoTree)
      val fullMixScriptHash = scorex.crypto.hash.Blake2b256(fullMixScriptBytes)
      env.setCollByte("fullMixScriptHash", fullMixScriptHash)
      val halfMixErgoTree = ScriptUtil.compile(env.toMap, halfMixScriptSource)

      fullMixScriptBytes.encodeHex shouldBe "1000d801d601e4c6a70507eb02cd7201cedb6a01dde4c6a70407e4c6a706077201"
      fullMixScriptBytes.encodeHex shouldBe fullMixErgoTree.bytes.encodeHex
      fullMixScriptHash.encodeHex shouldBe "dcd3cdd11102e7cb675fd5185a1685ad007619badcf398864274b137d9f45a9e"
      DefaultSerializer
        .serializeErgoTree(halfMixErgoTree)
        .encodeHex shouldBe "100804000e20dcd3cdd11102e7cb675fd5185a1685ad007619badcf398864274b137d9f45a9e040204020402040204020400d807d601b2a5730000d602c1a7d603e4c6a70407d6047301d605e4c672010507d606e4c672010407d607db6a01ddea02ea02d1ededededededed93c17201720293c1b2a5730200720293e4c672010607720393e4c6b2a57303000607720393cbc27201720493cbc2b2a5730400720493e4c6b2a57305000407720593e4c6b2a573060005077206eb02ce7207720372067205ce7207720372057206d193c5a7c5b2a4730700"
      DefaultSerializer.serializeErgoTree(halfMixErgoTree).encodeHex shouldBe halfMixErgoTree.bytes.encodeHex

      Pay2SAddress(fullMixErgoTree).toString shouldBe "TGzzrsmVRmFg5J5zMRYyRD2611Zj5ZQmXUmPkq2wwVpdjTryPpb"
      Pay2SAddress(halfMixErgoTree).toString shouldBe "5Dxxjran4RH9PP2AGnzwZp4poJeMJziukPfKzRbvMb8FGHKBCF4skzhmfhTaspTC4uCmNYNvZVTo1KxLCnGZ92nujEahkSacEhUTDGV84jmPP5pnc6zvkcHnaSxS1wbTbL4rcPBgMTdDesxQBSiJvmK2GCxrLmfzudGCABBR39ptKLb91Z6Q321HXs3oESZNEwpUixfNQQqF3RjsJQbpb5wiE3gwFgPmMRgPLCoBtwgk6Ly7zmm41xMhp1Um87DzbgdmFHZSupnVfiU98W8pg86aRzgpBAvDc6FZdeZKqDg4"
    }
  }

}
