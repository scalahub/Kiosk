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
          "9eiuh5bJtw9oWDVcfJnwTm1EHfK5949MEm5DStc2sD1TLwDSrpx", // private key is 37cc5cb5b54f98f92faef749a53b5ce4e9921890d9fb902b4456957d50791bd0
          "9f9q6Hs7vXZSQwhbrptQZLkTx15ApjbEkQwWXJqD2NpaouiigJQ", // private key is 5878ae48fe2d26aa999ed44437cffd2d4ba1543788cff48d490419aef7fc149d
          "9fGp73EsRQMpFC7xaYD5JFy2abZeKCUffhDBNbQVtBtQyw61Vym", // private key is 3ffaffa96b2fd6542914d3953d05256cd505d4beb6174a2601a4e014c3b5a78e
          "9fSqnSHKLzRz7sRkfwNW4Rqtmig2bHNaaspsQms1gY2sU6LA2Ng" // private key is 148bb91ada6ad5e6b1bba02fe70ecd96095e00cbaf0f1f9294f02fedf9855ea0
        ).toArray

        val oracle0PrivateKey = BigInt("37cc5cb5b54f98f92faef749a53b5ce4e9921890d9fb902b4456957d50791bd0", 16)
        val oracle1PrivateKey = BigInt("5878ae48fe2d26aa999ed44437cffd2d4ba1543788cff48d490419aef7fc149d", 16)
        val oracle2PrivateKey = BigInt("3ffaffa96b2fd6542914d3953d05256cd505d4beb6174a2601a4e014c3b5a78e", 16)
        val oracle3PrivateKey = BigInt("148bb91ada6ad5e6b1bba02fe70ecd96095e00cbaf0f1f9294f02fedf9855ea0", 16)

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

      val poolBootStrapTx: SignedTransaction = Box.$createTx(Array(dummyInputBox), Array[InputBox](), Array(epochPrepBoxToCreate), 1500000, changeAddress, Array[String](), Array[DhtData](), false)
      val epochPrepBox: InputBox = poolBootStrapTx.getOutputsToSpend.get(0)

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

      val createNewEpochTx = Box.$createTx(Array(epochPrepBox, dummyInputBox), Array[InputBox](), Array(liveEpochBoxToCreate), 1500000, changeAddress, Array[String](), Array[DhtData](), false)
      val liveEpochBox = createNewEpochTx.getOutputsToSpend.get(0)

      // create oracle box
      import ScalaErgoConverters._
      val r4oracle0 = KioskGroupElement(stringToGroupElement(ECC.addressToGroupElement(demoPool.addresses(0))))
      val r4oracle1 = KioskGroupElement(stringToGroupElement(ECC.addressToGroupElement(demoPool.addresses(1))))
      val r4oracle2 = KioskGroupElement(stringToGroupElement(ECC.addressToGroupElement(demoPool.addresses(2))))
      val r4oracle3 = KioskGroupElement(stringToGroupElement(ECC.addressToGroupElement(demoPool.addresses(3))))
      val r5oracle = KioskCollByte(Array(0x01))

      val oracle0Box = KioskBox(demoPool.dataPointAddress, value = 200000000, registers = Array(r4oracle0, r5oracle), tokens = Array(oracleToken))
      val oracle1Box = KioskBox(demoPool.dataPointAddress, value = 200000000, registers = Array(r4oracle1, r5oracle), tokens = Array(oracleToken))
      val oracle2Box = KioskBox(demoPool.dataPointAddress, value = 200000000, registers = Array(r4oracle2, r5oracle), tokens = Array(oracleToken))
      val oracle3Box = KioskBox(demoPool.dataPointAddress, value = 200000000, registers = Array(r4oracle3, r5oracle), tokens = Array(oracleToken))

      val bootStrapOracle0Tx = Box.$createTx(Array(dummyInputBox), Array[InputBox](), Array(oracle0Box), 1500000, changeAddress, Array[String](), Array[DhtData](), false)
      val oracleBox0ToSpend = bootStrapOracle0Tx.getOutputsToSpend.get(0)

      val bootStrapOracle1Tx = Box.$createTx(Array(dummyInputBox), Array[InputBox](), Array(oracle1Box), 1500000, changeAddress, Array[String](), Array[DhtData](), false)
      val oracleBox1ToSpend = bootStrapOracle1Tx.getOutputsToSpend.get(0)

      val bootStrapOracle2Tx = Box.$createTx(Array(dummyInputBox), Array[InputBox](), Array(oracle2Box), 1500000, changeAddress, Array[String](), Array[DhtData](), false)
      val oracleBox2ToSpend = bootStrapOracle2Tx.getOutputsToSpend.get(0)

      val bootStrapOracle3Tx = Box.$createTx(Array(dummyInputBox), Array[InputBox](), Array(oracle3Box), 1500000, changeAddress, Array[String](), Array[DhtData](), false)
      val oracleBox3ToSpend = bootStrapOracle3Tx.getOutputsToSpend.get(0)

      // commit dataPoints
      val r5dataPoint = KioskCollByte(liveEpochBox.getId.getBytes)
      val r6dataPoint0 = KioskLong(123)
      val r6dataPoint1 = KioskLong(200)
      val r6dataPoint2 = KioskLong(100)
      val r6dataPoint3 = KioskLong(199)

      val commitBox0ToCreate = KioskBox(
        demoPool.dataPointAddress,
        value = 200000000,
        registers = Array(r4oracle0, r5dataPoint, r6dataPoint0),
        tokens = Array(oracleToken)
      )

      val createDataPoint0Tx = Box.$createTx(
        Array(oracleBox0ToSpend, dummyInputBox),
        Array(liveEpochBox),
        Array(commitBox0ToCreate),
        1500000,
        changeAddress,
        Array[String](demoPool.oracle0PrivateKey.toString),
        Array[DhtData](),
        false
      )

      val commitBox0 = createDataPoint0Tx.getOutputsToSpend.get(0)

      val commitBox1ToCreate = KioskBox(
        demoPool.dataPointAddress,
        value = 200000000,
        registers = Array(r4oracle1, r5dataPoint, r6dataPoint1),
        tokens = Array(oracleToken)
      )

      val createDataPoint1Tx = Box.$createTx(
        Array(oracleBox1ToSpend, dummyInputBox),
        Array(liveEpochBox),
        Array(commitBox1ToCreate),
        1500000,
        changeAddress,
        Array[String](demoPool.oracle1PrivateKey.toString),
        Array[DhtData](),
        false
      )

      val commitBox1 = createDataPoint1Tx.getOutputsToSpend.get(0)

      val commitBox2ToCreate = KioskBox(
        demoPool.dataPointAddress,
        value = 200000000,
        registers = Array(r4oracle2, r5dataPoint, r6dataPoint2),
        tokens = Array(oracleToken)
      )

      val createDataPoint2Tx = Box.$createTx(
        Array(oracleBox2ToSpend, dummyInputBox),
        Array(liveEpochBox),
        Array(commitBox2ToCreate),
        1500000,
        changeAddress,
        Array[String](demoPool.oracle2PrivateKey.toString),
        Array[DhtData](),
        false
      )

      val commitBox2 = createDataPoint2Tx.getOutputsToSpend.get(0)

      val commitBox3ToCreate = KioskBox(
        demoPool.dataPointAddress,
        value = 200000000,
        registers = Array(r4oracle3, r5dataPoint, r6dataPoint3),
        tokens = Array(oracleToken)
      )

      val createDataPoint3Tx = Box.$createTx(
        Array(oracleBox3ToSpend, dummyInputBox),
        Array(liveEpochBox),
        Array(commitBox3ToCreate),
        1500000,
        changeAddress,
        Array[String](demoPool.oracle3PrivateKey.toString),
        Array[DhtData](),
        false
      )

      val commitBox3 = createDataPoint3Tx.getOutputsToSpend.get(0)

      // collect dataPoints
      val dataPointPairs = Array(
        (commitBox0, r6dataPoint0, demoPool.addresses(0)),
        (commitBox1, r6dataPoint1, demoPool.addresses(1)),
        (commitBox2, r6dataPoint2, demoPool.addresses(2)),
        (commitBox3, r6dataPoint3, demoPool.addresses(3))
      )

      val collectDataInputs = dataPointPairs.unzip3._1

      val average = dataPointPairs.unzip3._2.map(_.value).sum / dataPointPairs.length

      val r4collect = KioskLong(average)
      val r5collect = KioskInt(r5liveEpoch.value + demoPool.epochPeriod)

      val epoch1PrepBoxToCreate = KioskBox(
        demoPool.epochPrepAddress,
        liveEpochBoxToCreate.value, // - demoPool.addresses.length * demoPool.oracleReward,
        Array(r4collect, r5collect),
        liveEpochBoxToCreate.tokens
      )

      val rewards = dataPointPairs.unzip3._3.map { address =>
        KioskBox(address, demoPool.oracleReward, Array(), Array())
      }

      val createCollectTx = Box.$createTx(
        Array(liveEpochBox),
        collectDataInputs,
        Array(epoch1PrepBoxToCreate) ++ rewards,
        1500000,
        changeAddress,
        Array[String](demoPool.oracle0PrivateKey.toString),
        Array[DhtData](),
        false
      )

    }

  }

}
