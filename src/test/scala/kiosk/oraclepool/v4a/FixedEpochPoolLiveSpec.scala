package kiosk.oraclepool.v4a

import kiosk.ErgoUtil
import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo._
import kiosk.tx.TxUtil
import org.ergoplatform.appkit._
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scorex.crypto.hash.Blake2b256

class FixedEpochPoolLiveSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {
  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))

  property("One complete epoch") {

    ergoClient.execute { implicit ctx: BlockchainContext =>
      val pool = new FixedEpochPoolLive {
        lazy val addresses = Seq(
          "9eiuh5bJtw9oWDVcfJnwTm1EHfK5949MEm5DStc2sD1TLwDSrpx", // private key is 37cc5cb5b54f98f92faef749a53b5ce4e9921890d9fb902b4456957d50791bd0
          "9f9q6Hs7vXZSQwhbrptQZLkTx15ApjbEkQwWXJqD2NpaouiigJQ", // private key is 5878ae48fe2d26aa999ed44437cffd2d4ba1543788cff48d490419aef7fc149d
          "9fGp73EsRQMpFC7xaYD5JFy2abZeKCUffhDBNbQVtBtQyw61Vym", // private key is 3ffaffa96b2fd6542914d3953d05256cd505d4beb6174a2601a4e014c3b5a78e
          "9fSqnSHKLzRz7sRkfwNW4Rqtmig2bHNaaspsQms1gY2sU6LA2Ng" // private key is 148bb91ada6ad5e6b1bba02fe70ecd96095e00cbaf0f1f9294f02fedf9855ea0
        ).toArray

        val oracle0PrivateKey: scala.BigInt = scala.BigInt("37cc5cb5b54f98f92faef749a53b5ce4e9921890d9fb902b4456957d50791bd0", 16)
        val oracle1PrivateKey: scala.BigInt = scala.BigInt("5878ae48fe2d26aa999ed44437cffd2d4ba1543788cff48d490419aef7fc149d", 16)
        val oracle2PrivateKey: scala.BigInt = scala.BigInt("3ffaffa96b2fd6542914d3953d05256cd505d4beb6174a2601a4e014c3b5a78e", 16)
        val oracle3PrivateKey: scala.BigInt = scala.BigInt("148bb91ada6ad5e6b1bba02fe70ecd96095e00cbaf0f1f9294f02fedf9855ea0", 16)
      }

      val fee = 1500000

      val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"
      val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
      val dummyScript = "{sigmaProp(1 < 2)}"
      val poolToken = (pool.poolToken, 1L)
      val oracleToken = (pool.oracleToken, 1L)

      // bootstrap pool (create EpochPrep box)
      val r4epochPrep = KioskLong(100) // dummy data point
      val r5epochPrep = KioskInt(20000) // end height of epoch

      val epochPrepBoxToCreate = KioskBox(
        pool.epochPrepAddress,
        value = 2000000000,
        registers = Array(r4epochPrep, r5epochPrep),
        tokens = Array(poolToken)
      )

      val dummyPoolToken = new ErgoToken(pool.poolToken, 10000)
      val dummyOracleToken = new ErgoToken(pool.oracleToken, 10000)
      val customInputBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(10000000000000L)
        .tokens(dummyOracleToken, dummyPoolToken)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .build()
        .convertToInputWith(dummyTxId, 0)

      val poolBootStrapTx: SignedTransaction = TxUtil.createTx(Array(customInputBox), Array[InputBox](), Array(epochPrepBoxToCreate), fee, changeAddress, Array[String](), Array[DhtData](), false)
      val epochPrepBox: InputBox = poolBootStrapTx.getOutputsToSpend.get(0)

      // create new epoch
      val r4liveEpoch = r4epochPrep
      val r5liveEpoch = KioskInt(ctx.getHeight + pool.epochPeriod + pool.buffer) // end height of epoch
      val r6liveEpoch = KioskCollByte(Blake2b256(pool.epochPrepErgoTree.bytes))

      val liveEpochBoxToCreate = KioskBox(
        pool.liveEpochAddress,
        value = 2000000000,
        registers = Array(r4liveEpoch, r5liveEpoch, r6liveEpoch),
        tokens = Array(poolToken)
      )

      val createNewEpochTx = TxUtil.createTx(Array(epochPrepBox, customInputBox), Array[InputBox](), Array(liveEpochBoxToCreate), fee, changeAddress, Array[String](), Array[DhtData](), false)
      val liveEpochBox = createNewEpochTx.getOutputsToSpend.get(0)

      // create oracle box
      import ScalaErgoConverters._
      val r4oracle0 = KioskGroupElement(stringToGroupElement(ErgoUtil.addressToGroupElement(pool.addresses(0))))
      val r4oracle1 = KioskGroupElement(stringToGroupElement(ErgoUtil.addressToGroupElement(pool.addresses(1))))
      val r4oracle2 = KioskGroupElement(stringToGroupElement(ErgoUtil.addressToGroupElement(pool.addresses(2))))
      val r4oracle3 = KioskGroupElement(stringToGroupElement(ErgoUtil.addressToGroupElement(pool.addresses(3))))
      val r5oracle = KioskCollByte(Array(0x01))

      val oracle0Box = KioskBox(pool.dataPointAddress, value = 200000000, registers = Array(r4oracle0, r5oracle), tokens = Array(oracleToken))
      val oracle1Box = KioskBox(pool.dataPointAddress, value = 200000000, registers = Array(r4oracle1, r5oracle), tokens = Array(oracleToken))
      val oracle2Box = KioskBox(pool.dataPointAddress, value = 200000000, registers = Array(r4oracle2, r5oracle), tokens = Array(oracleToken))
      val oracle3Box = KioskBox(pool.dataPointAddress, value = 200000000, registers = Array(r4oracle3, r5oracle), tokens = Array(oracleToken))

      val bootStrapOracle0Tx = TxUtil.createTx(Array(customInputBox), Array[InputBox](), Array(oracle0Box), fee, changeAddress, Array[String](), Array[DhtData](), false)
      val oracleBox0ToSpend = bootStrapOracle0Tx.getOutputsToSpend.get(0)

      val bootStrapOracle1Tx = TxUtil.createTx(Array(customInputBox), Array[InputBox](), Array(oracle1Box), fee, changeAddress, Array[String](), Array[DhtData](), false)
      val oracleBox1ToSpend = bootStrapOracle1Tx.getOutputsToSpend.get(0)

      val bootStrapOracle2Tx = TxUtil.createTx(Array(customInputBox), Array[InputBox](), Array(oracle2Box), fee, changeAddress, Array[String](), Array[DhtData](), false)
      val oracleBox2ToSpend = bootStrapOracle2Tx.getOutputsToSpend.get(0)

      val bootStrapOracle3Tx = TxUtil.createTx(Array(customInputBox), Array[InputBox](), Array(oracle3Box), fee, changeAddress, Array[String](), Array[DhtData](), false)
      val oracleBox3ToSpend = bootStrapOracle3Tx.getOutputsToSpend.get(0)

      val r5dataPoint = KioskCollByte(liveEpochBox.getId.getBytes)

      val change = KioskBox(
        "9fcrXXaJgrGKC8iu98Y2spstDDxNccXSR9QjbfTvtuv7vJ3NQLk",
        414500000,
        Array(),
        Array((pool.oracleToken, 490))
      )

      type DataPointBox = InputBox
      type DataPoint = KioskLong
      type Address = String
      type PrivateKey = BigInt

      // dataPoints to commit
      val r6dataPoint0: DataPoint = KioskLong(100)
      val r6dataPoint1: DataPoint = KioskLong(102)
      val r6dataPoint2: DataPoint = KioskLong(103)
      val r6dataPoint3: DataPoint = KioskLong(105)

      val dataPointInfo3 = Array(
        (oracleBox1ToSpend, r4oracle1, r6dataPoint1, pool.addresses(1), pool.oracle1PrivateKey),
        (oracleBox2ToSpend, r4oracle2, r6dataPoint2, pool.addresses(2), pool.oracle2PrivateKey),
        (oracleBox3ToSpend, r4oracle3, r6dataPoint3, pool.addresses(3), pool.oracle3PrivateKey)
      )

      val dataPointInfoAll = Array(
        (oracleBox0ToSpend, r4oracle0, r6dataPoint0, pool.addresses(0), pool.oracle0PrivateKey),
        (oracleBox1ToSpend, r4oracle1, r6dataPoint1, pool.addresses(1), pool.oracle1PrivateKey),
        (oracleBox2ToSpend, r4oracle2, r6dataPoint2, pool.addresses(2), pool.oracle2PrivateKey),
        (oracleBox3ToSpend, r4oracle3, r6dataPoint3, pool.addresses(3), pool.oracle3PrivateKey)
      )

      assert(liveEpochBox.getErgoTree.bytes.encodeHex == pool.liveEpochErgoTree.bytes.encodeHex)

      // collect three dataPoints
      commitAndCollect(dataPointInfo3)

      // collect All dataPoints
      commitAndCollect(dataPointInfoAll)

      def commitDataPoint(dataPointBox: DataPointBox, r4dataPoint: KioskGroupElement, r6dataPoint: DataPoint, oraclePrivateKey: PrivateKey) = {
        val commitBoxToCreate = KioskBox(
          pool.dataPointAddress,
          value = 200000000,
          registers = Array(r4dataPoint, r5dataPoint, r6dataPoint),
          tokens = Array(oracleToken)
        )
        val createDataPointTx = TxUtil.createTx(
          Array(dataPointBox, customInputBox),
          Array(liveEpochBox),
          Array(commitBoxToCreate),
          1500000,
          changeAddress,
          Array[String](oraclePrivateKey.toString),
          Array[DhtData](),
          false
        )
        createDataPointTx.getOutputsToSpend.get(0)
      }

      def collect(dataPointBoxes: Array[DataPointBox], dataPoints: Array[DataPoint], addresses: Array[Address], privateKey: PrivateKey) = { //Array[(DataPointBox, KioskGroupElement, DataPoint, Address, PrivateKey)]) = {

        val privateKeys: Array[Option[PrivateKey]] = addresses.zipWithIndex.map {
          case (_, 0) => Some(privateKey)
          case _      => None
        }

        val tuples: Array[(DataPointBox, DataPoint, Address, Option[PrivateKey])] = (dataPointBoxes zip dataPoints) zip (addresses zip privateKeys) map {
          case ((dataPointBox, dataPoint), (address, optPrivateKey)) =>
            (dataPointBox, dataPoint, address, optPrivateKey)
        } sortBy (-_._2.value)

        val dataPointBoxesSorted: Array[DataPointBox] = tuples.map(_._1)

        val epoch1PrepBoxToCreate = KioskBox(
          pool.epochPrepAddress,
          liveEpochBoxToCreate.value - (dataPoints.length + 1) * pool.oracleReward,
          Array(KioskLong(dataPoints.map(_.value).sum / dataPoints.length), KioskInt(r5liveEpoch.value + pool.epochPeriod)),
          liveEpochBoxToCreate.tokens
        )

        var myIndex = 0

        val rewardBoxes: Array[KioskBox] = tuples.zipWithIndex.map {
          case ((_, _, address, Some(_)), i) =>
            myIndex = i
            KioskBox(address, pool.oracleReward * 2, Array(), Array())
          case ((_, _, address, _), _) =>
            KioskBox(address, pool.oracleReward, Array(), Array())
        }
        rewardBoxes(0) = rewardBoxes(0).copy(registers = Array(KioskInt(myIndex)))

        TxUtil.createTx(
          Array(liveEpochBox, customInputBox),
          dataPointBoxesSorted,
          Array(epoch1PrepBoxToCreate) ++ rewardBoxes ++ Array(change),
          fee,
          changeAddress,
          Array[String](privateKey.toString),
          Array[DhtData](),
          false
        )
      }

      def commitAndCollect(dataPointInfo: Array[(DataPointBox, KioskGroupElement, DataPoint, Address, PrivateKey)]) = {
        val dataPointPairs: Array[((DataPointBox, DataPoint), (Address, PrivateKey))] = dataPointInfo.map {
          case (dataPointBox, kioskGroupElement, dataPoint, address, privateKey) =>
            val commitBox = commitDataPoint(dataPointBox, kioskGroupElement, dataPoint, privateKey)
            ((commitBox, dataPoint), (address, privateKey))
        }

        val collectDataInputs: Array[DataPointBox] = dataPointPairs.unzip._1.unzip._1
        val dataPoints: Array[DataPoint] = dataPointPairs.unzip._1.unzip._2
        val addresses: Array[Address] = dataPointPairs.unzip._2.unzip._1
        val privateKey: PrivateKey = dataPointPairs.unzip._2.unzip._2(0)

        collect(collectDataInputs, dataPoints, addresses, privateKey)
      }

    }

  }

}
