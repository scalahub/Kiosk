package org.sh.kiosk.ergo

import kiosk.ErgoUtil
import kiosk.ergo._
import kiosk.script.ScriptUtil
import org.ergoplatform.Pay2SAddress
import org.scalatest.{Matchers, WordSpec}
import scorex.crypto.hash.Blake2b256
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer

import scala.collection.mutable.{Map => MMap}

class YieldProtocolSpec extends WordSpec with Matchers with YieldProtocol {

  "InterestFreeLoan script" should {
    "compile correctly" in {
      val rateOracleTokenID: Array[Byte] = Blake2b256("rate").toArray // To use the correct id in real world
      // issuer
      val alicePrivateKey = BigInt("75374758593378829465462861800432295633548137326937261134135717651967631579194")
      val alice = ErgoUtil.gX(alicePrivateKey)

      val env = MMap[String, KioskType[_]]()

      import ScriptUtil._

      env.setCollByte("rateTokenID", rateOracleTokenID)
      env.setGroupElement("alice", alice)

      val liquidatedBoxErgoTree = ScriptUtil.compile(env.toMap, liquidatedBoxSrc)

      val liquidatedBoxScriptBytes = DefaultSerializer.serializeErgoTree(liquidatedBoxErgoTree)
      val liquidatedBoxScriptHash = scorex.crypto.hash.Blake2b256(liquidatedBoxScriptBytes)

      env.setCollByte("liquidatedBoxScriptHash", liquidatedBoxScriptHash)

      val maxBonds = 100000000L
      val minBondsToPurchase = 1000L
      val endHeight = 100000 // height at which bond ends
      val withdrawDeadline = 100000 // minimum delay given to withdraw

      env.setLong("maxBonds", maxBonds)
      env.setLong("minBondsToPurchase", minBondsToPurchase)
      env.setInt("endHeight", endHeight)
      env.setLong("withdrawDeadline", withdrawDeadline)

      val bondBoxErgoTree = ScriptUtil.compile(env.toMap, bondBoxSource)

      liquidatedBoxErgoTree.bytes.encodeHex shouldBe "100504000400040008cd02adee2f931b2e011fc44c9ece6ad6ad2e2dafe6506114755f20bad7268b191a3d05a09c01d806d601e4c6a70504d602b2a5730000d603b2db63087202730100d604b2db6308a7730200d605998c7203028c720402d606e4c6a70405eb02ea027303d191a37201d1ededededed93c27202c2a7938c7203018c72040192720573049099c1a7c172029c7205720693e4c672020405720693e4c6720205047201"
      bondBoxErgoTree.bytes.encodeHex shouldBe "100b040004000400040004c09a0c051405160e20650279277bc6f6c88ff1e403446ae375a20f1ea30d7b9a4aa0f01be336e66b1505c09a0c05d00f058084af5fd808d601c1a7d602e4c6a70405d603e4c6b2db6501fe7300000405d604b2a5730100d605b2db6308a7730200d606b2db63087204730300d6078c720602d6088c72050295ec91a37304929c720173059c9c720272037306edededed93cbc272047307938c7205018c720601937207720893e4c6720404057203927ee4c672040504059a7ea3057308d802d609e4c672040405d60a9972097202edededed93c27204c2a792720a730993720a99720872079299c1720472019c720a7203907209730a"
      Pay2SAddress(liquidatedBoxErgoTree).toString shouldBe "9mBmnHeq2w8zrEXrmFueRb92q2whmHD1LBUjdwp3ikBZPJ99GUgzizwWnMt32w3dEuKBC52vkFmmDXiRr8W6dcBspBk4DefKACTSYyPm46xrpVf6Ev5FknXBtfQghmHT42EX9TqYSs7w2CkS9JL5jG4jaqNdUk2BuDjacerrRz9fS1Z8BEcCXzHsJSQmLwFDW17osdBq4SxMVLrgGL8QYXS7CYNFEWjRcjAhbamWUMHZk"
      Pay2SAddress(bondBoxErgoTree).toString shouldBe "K3dYCmBVRgBZ2Z5MrLJxbMiPUCwVMXEqak85wuWBGL2TwdhCv7CBeiHBvGQGhwqyzoHXvNuMGj2oWB5CELaC19nXYZAKJyVUUs6YvMdAtDyvtzJQdzecs5x9TrWKZkoZpAggDkqC73y1Ct318QB9gKLvDUccWMzRUoRmTkiFb9XWeys4x4UyMjCgwEbZDkj4oRmBX1ULpCZyhewvNC5HC5XJveRJUTRFKaSxUt6t83BnTMmKc5gnMnhTAhZJkjpVUiGXUSi4zpEoTpWsR5Svxxf418JjdF8mjfejQ2h9T5xY9hhDhfFXm8utPGCj3Fb6pC4zuhpzA2qoCB75aJ1fcpWJbh2qidfD9HGEp"
    }
  }
}
