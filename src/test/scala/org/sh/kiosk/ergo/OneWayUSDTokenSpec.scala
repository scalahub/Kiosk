package org.sh.kiosk.ergo

import kiosk.ErgoUtil
import kiosk.ergo._
import kiosk.script.ScriptUtil
import org.ergoplatform.Pay2SAddress
import org.scalatest.{Matchers, WordSpec}
import scorex.crypto.hash.Blake2b256

import scala.collection.mutable.{Map => MMap}

class OneWayUSDTokenSpec extends WordSpec with Matchers with OneWayUSDToken {

  "OneWayUSDToken script" should {
    "compile correctly" in {
      val rateOracleTokenID: Array[Byte] = Blake2b256("rate").toArray // To use the correct id in real world

      val env = MMap[String, KioskType[_]]()

      import ScriptUtil._

      env.setCollByte("rateTokenID", rateOracleTokenID)

      // lender
      val bobPrivateKey = BigInt("75374758593378829465462861800432295633548137326937261134135717651967631579194")
      val bob = ErgoUtil.gX(bobPrivateKey)

      env.setGroupElement("bob", bob)

      val ergoTree = ScriptUtil.compile(env.toMap, source)

      ergoTree.bytes.encodeHex shouldBe "10080400040004000402040004000e20ef3a6cba7fae67bdef5c7acf35658e22c3768c5d282771c61f5675f4a283c41908cd02adee2f931b2e011fc44c9ece6ad6ad2e2dafe6506114755f20bad7268b191a3dd805d601b2db6308a7730000d602b2a5730100d603b2db63087202730200d604b2a5730300d605b2db6501fe730400ededed90998c7201028c7203029dc17204e4c672050405938cb2db63087205730500017306ed938c7201018c72030193c27202c2a793c27204d07307"

      import ScriptUtil.ergoAddressEncoder
      Pay2SAddress(ergoTree).toString shouldBe "3Nvu37NpxyjZ4ppVxRBuxMdM2Z9m34KHAds1fr4WTm39XJNXnbLrffMfzzQNZTec6tsKGqDKXg3RFfLRASYRmkaM36nf9McJF1GLG72krbTfL64XadZ53aeNK6NWHTpyQMkrcVrQPiooaBm4aYfXnfyroFdCJPwmAaCL6RgCq7RrVXfyUZcuuaspTJ8hmiECn4Ke7Eio7u2JvLJRaHKQ4mXHSHZtjFQETSMJRDsYDQv67QBXGYfwc7hiEFfeTP2B31BaUPdCTc"
    }
  }
}
