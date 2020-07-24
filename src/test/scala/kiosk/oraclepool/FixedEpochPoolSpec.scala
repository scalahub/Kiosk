package kiosk.oraclepool

import kiosk.encoding.ScalaErgoConverters
import kiosk.{Box, ECC}
import kiosk.ergo._
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, HttpClientTesting, InputBox, SignedTransaction}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class FixedEpochPoolSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {

  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))

  property("OraclePool") {

    ergoClient.execute { implicit ctx: BlockchainContext =>
      val demoPool = new FixedEpochPool {
        val minBoxValue = 2000000
        override lazy val livePeriod = 4 // blocks
        override lazy val prepPeriod = 4 // blocks
        override lazy val buffer = 2 // blocks

        lazy val oracleToken = "12caaacb51c89646fac9a3786eb98d0113bd57d68223ccc11754a4f67281daed"
        lazy val poolToken = "961c8d498431664f4fb8a660b9a62618f092e34ef07370ba1a2fb7c278c5f57d"

        override lazy val oracleTokenId: Array[Byte] = oracleToken.decodeHex
        override lazy val poolTokenId: Array[Byte] = poolToken.decodeHex

        override lazy val oracleReward = 2000000 // Nano ergs. One reward per data point to be paid to oracle
        lazy val addresses = Seq(
          "9iHunbPfq8ARpiJXc4vjmwvseeHWjmgeC797vSrdHSLNKxvKsYo",
          "9fj9NJpzo13HfNyCdzyfNP8zAfjiTY3pys1JP5wCzez8MiP8QbF",
          "9ebeQK9oJpDpTZSfqk6wdaHt3x1aUUba9S8dMufTpyQQYvE2XKU",
          "9fSqnSHKLzRz7sRkfwNW4Rqtmig2bHNaaspsQms1gY2sU6LA2Ng" // private key is 148bb91ada6ad5e6b1bba02fe70ecd96095e00cbaf0f1f9294f02fedf9855ea0
        ).toArray

        val oracle4PrivateKey = BigInt("148bb91ada6ad5e6b1bba02fe70ecd96095e00cbaf0f1f9294f02fedf9855ea0", 16)

        override lazy val minPoolBoxValue = oracleReward * (addresses.size + 1) + minBoxValue // how much min must exist in oracle pool box
        override lazy val oraclePubKeys: Array[GroupElementHex] = addresses.map(ECC.addressToGroupElement)
      }

      val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"
      val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
      val dummyScript = "{sigmaProp(1 < 2)}"
      val poolToken = (demoPool.poolToken, 1L)
      val oracleToken = (demoPool.oracleToken, 1L)

      // bootstrap pool (create EpochPrep box)
      val r4epochPrep = KioskLong(1) // dummy data point
      val r5epochPrep = KioskInt(20000) // end height of epoch

      val epochPrepBoxToCreate = KioskBox(
        demoPool.epochPrepAddress,
        value = 2000000000,
        registers = Array(r4epochPrep, r5epochPrep),
        tokens = Array(poolToken)
      )

      val dummyInputBox = ctx.newTxBuilder().outBoxBuilder.value(10000000000000L).contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript)).build().convertToInputWith(dummyTxId, 0)

      val poolBootStrapTx: SignedTransaction = Box.$createTx(Array(dummyInputBox), Array(epochPrepBoxToCreate), 1500000, changeAddress, Array[String](), Array[DhtData](), false)
      val epochPrepBox: InputBox = poolBootStrapTx.getOutputsToSpend.get(0)
      println(poolBootStrapTx.toJson(false))

      // create new epoch
      val r4liveEpoch = r4epochPrep
      val r5liveEpoch = KioskInt(ctx.getHeight + demoPool.epochPeriod + demoPool.buffer) // end height of epoch
      val r6liveEpoch = KioskCollByte(demoPool.epochPrepErgoTree.bytes)

      val liveEpochBoxToCreate = KioskBox(
        demoPool.liveEpochAddress,
        value = 2000000000,
        registers = Array(r4liveEpoch, r5liveEpoch, r6liveEpoch),
        tokens = Array(poolToken)
      )

      val createNewEpochTx = Box.$createTx(Array(epochPrepBox, dummyInputBox), Array(liveEpochBoxToCreate), 1500000, changeAddress, Array[String](), Array[DhtData](), false)
      println(createNewEpochTx.toJson(false))

      // create oracle box
      import ScalaErgoConverters._
      val r4oracle = KioskGroupElement(stringToGroupElement(ECC.addressToGroupElement(demoPool.addresses(3))))
      val r5oracle = KioskCollByte(Array(0x01))
      val oracleBox = KioskBox(
        demoPool.dataPointAddress,
        value = 200000000,
        registers = Array(r4oracle, r5oracle),
        tokens = Array(oracleToken)
      )

      val bootStrapOracleTx = Box.$createTx(Array(dummyInputBox), Array(oracleBox), 1500000, changeAddress, Array[String](), Array[DhtData](), false)
      println(bootStrapOracleTx.toJson(false))

    }

  }

}
