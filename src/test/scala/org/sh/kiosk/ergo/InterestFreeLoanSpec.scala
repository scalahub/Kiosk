package org.sh.kiosk.ergo

import kiosk.ErgoUtil
import kiosk.ergo._
import kiosk.script.ScriptUtil
import org.ergoplatform.Pay2SAddress
import org.scalatest.{Matchers, WordSpec}
import scorex.crypto.hash.Blake2b256

import scala.collection.mutable.{Map => MMap}

class InterestFreeLoanSpec extends WordSpec with Matchers with InterestFreeLoan {

  "InterestFreeLoan script" should {
    "compile correctly" in {
      val rateOracleTokenID: Array[Byte] = Blake2b256("rate").toArray // gives rate in nanoErgs per usdCent
      val usdTokenID: Array[Byte] = Blake2b256("USD").toArray // bob's one-way USD token

      val env = MMap[String, KioskType[_]]()

      import ScriptUtil._

      env.setCollByte("rateOracleTokenID", rateOracleTokenID)
      env.setCollByte("usdTokenID", usdTokenID)

      // borrower
      val alicePrivateKey = BigInt("75374758593378829465462861800432295633548137326937261134135717651967631579194")
      val alice = ErgoUtil.gX(alicePrivateKey)

      // lender
      val bobPrivateKey = BigInt("12312431413445636473523563674877986978905435274658567967890258769680652773488")
      val bob = ErgoUtil.gX(bobPrivateKey)

      val oneMonth = 720 * 30 // 720 blocks per day
      val fiveDays = 720 * 5 // 720 blocks per day

      val startDate = 10000 // block height at which loan was started

      env.setGroupElement("alice", alice)
      env.setGroupElement("bob", bob)
      env.setInt("oneMonth", oneMonth)
      env.setInt("fiveDays", fiveDays)
      env.setInt("emi", 1000) // equal monthly installment in USD cents

      val ergoTree = ScriptUtil.compile(env.toMap, src)
      ergoTree.bytes.encodeHex shouldBe "101404000518051405040400040208cd02b4257a90a74d2c596aad692454533e265d30b5ac5cba7503e950f3aa98aa39a80702adee2f931b2e011fc44c9ece6ad6ad2e2dafe6506114755f20bad7268b191a3d05d00f04000e20ef3a6cba7fae67bdef5c7acf35658e22c3768c5d282771c61f5675f4a283c41904a00b040005000500040404040e2013080243c81647060bf7c8ebb7df10fc0aee16274810abf91f8c85f5c819d88805d00f04e08903d813d601c1a7d602e4c6a70405d603b2db6501fe730000d604e4c672030405d6059c72027204d6069d9c720573017302d6079d99720172067303d608b2a5730400d60993c27208c2a7d60ac17208d60b997202e4c672080405d60cb2a5730500d60d7306d60e93c2720cd0720dd60f7307d610e4c6a70504d611e4c672080504d612d1edededed93720b7308939c720b7204997201720a7209938cb2db6308720373090001730aeded907211a392721199a3730b9172117210d613b2db6308720c730c00eb02d1edededededededed917207730d720993720a720693720b730e720e93c1720c720793c2b2a5730f00d0cd720f93c1b2a573100072079372107211eb03ea02d19172057201720dea02ea027212cd720fd1eded938c7213017311938c7213027312720eea02ea027212720dd19199a372107313"

      import ScriptUtil.ergoAddressEncoder
      Pay2SAddress(ergoTree).toString shouldBe "A9esikT7NVU4Epn4JCG6Jq2CMht7YttLszCx6UtjDvoam5czQNP1NnqgFqb7i5Yw8DFsUuqBAumixiYa7uyc8s2Rf3DZPuZ3s22qHnitM5U3ajq5oSTvN2JKDir2cctWpkxVqVWQXCC9yafuorma6t7RQwr6CecfuTSnimwsvrJVfoYNVmpEdVCaiqxPdHMsM3XW7oxmRsVjRqwna3xWbqQ1Fb8uEuHgteGYzkdwBUKjEDg2FtygQzsv5dw5S4ncs5miYe8mS2zCy5KgzCbD8mCAmHBDANCJ3ZdPB55iSYhNWnmAqkQANdyRArHNjaV7QZSAmJ8AXFheRJvsAk8b8TNi97A6avKRJAuLTA5osR7f5pvXc1nD94eUz6F8VHxANBgkvjZkc6We85971MikjnvwAtMzgKvszvjEsWCZb5tmUXXZYZMLfgEhwKqUeCQjt8Dgz74xt5RW7RDBTnKM3LUcux3tpf9w3mUDfiufBjQXRZoZ7T5F9nbau4fySe7frWz232t7zG72JA8K4NvkVz53V3Brie55Ufyuo9dmW1WgnvdxxbSLVYZmDzD6hTbMeapYX1GkCFLsn5UVNrJWN4KTuPt6LQCCtkdrzWf1XfSGd2p4jVXszLCCNUKbNm1ARrxUPGwApBL5pFzfF8WPL7BTYw8i2tPmVy"
    }
  }
}
