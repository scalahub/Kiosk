package org.sh.kiosk.ergo

import kiosk.ErgoUtil
import kiosk.ergo._
import kiosk.script.ScriptUtil
import org.ergoplatform.Pay2SAddress
import org.scalatest.{Matchers, WordSpec}

import scala.collection.mutable.{Map => MMap}

class AuctionSpec extends WordSpec with Matchers with Auction {

  import kiosk.script.ScriptUtil._

  "Auction script" should {
    "compile correctly" in {
      // seller
      val alicePrivateKey = BigInt("75374758593378829465462861800432295633548137326937261134135717651967631579194")
      val alice = ErgoUtil.gX(alicePrivateKey)

      val env = MMap[String, KioskType[_]]()
      env.setGroupElement("alice", alice)
      val ergoTree = ScriptUtil.compile(env.toMap, source)

      ergoTree.bytes.encodeHex shouldBe "100b04904e04000400040004020480897a040004000400040208cd02adee2f931b2e011fc44c9ece6ad6ad2e2dafe6506114755f20bad7268b191a3dd802d601c1a7d602e4c6a70407958fa37300d805d603b2a5730100d604b2db63087203730200d605b2db6308a7730300d606c17203d607b2a5730400ededededed938c7204018c720501938c7204028c7205029272069a72017e730505927206720693c27207d0cd720292c172077201d804d603b2a5730600d604b2db63087203730700d605b2db6308a7730800d606b2a5730900edededed938c7204018c720501938c7204028c72050293c27203d0cd720292c17206720193c27206d0730a"

      import ScriptUtil.ergoAddressEncoder
      Pay2SAddress(ergoTree).toString shouldBe "dEnpB3TwiN24zJ6u2mDgShn2PBB11ZMr1GLGpsTDx8BJnY2RfkuB8nYMKkuhzaN83zJ8bbDFv3vV4xH8zrWFVbpjPJ7Evu4xsfMQmnReHaZCuCAoWKPkzeSXRiL3DnK26ZkFoxso6TNuoYCkCDNXFuU4CTqjQzCB1bMYxeGxybCVUZWzMHBFqGZFkFuYcsxtBFabipgedhVjGcrCtQfdPNqz7ZUZ5mrSSzALdRJ8SgfvESnzM3eHSYRc21GEf9P3HzjgFWuhtyX3KuYfiPjDx3tZfTsrA9RsHJGvtGmkPVSwgoahWCQ6YnZZf888HkDbsCmv9QM877YPvhVKkdnbaans6Pu2LG"
    }
  }
}
