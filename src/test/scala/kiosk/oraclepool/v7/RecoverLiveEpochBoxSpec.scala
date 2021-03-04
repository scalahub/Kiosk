package kiosk.oraclepool.v7

import kiosk.ErgoUtil
import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo.{ByteArrayToBetterByteArray, DhtData, KioskBox, KioskCollByte, KioskGroupElement, KioskInt, KioskLong}
import kiosk.tx.TxUtil
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, ErgoToken, HttpClientTesting, InputBox}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scorex.crypto.hash.Blake2b256

/*
 * This will test that the oracle-pool does not go into a locked state due to non-existence of data-point boxes
 * Earlier the data-point script required all the following conditions:
 *  1. The data-input box contains the pool NFT
 *  2. R4 of the output data-point box contains the data value
 *  3. R5 of the output data-point box contains the box id of data-input
 *  4. The script of data-input equals live epoch script hash
 *
 *  The fixed script has the following conditions (#4 is removed):
 *  1. The data-input box contains the pool NFT
 *  2. R4 of the output data-point box contains the data value
 *  3. R5 of the output data-point box contains the box id of data-input
 *
 * This will allow the data-point box to be attached to any box that has the pool NFT, which could be any box
 *
 * To produce the lock condition do the following
 *
 * 1. Have a functioning pool and collect data points (old contract)
 * 2. Update epoch prep box to (new contract)
 * 3. Put pool in live epoch state (new contract)
 * 4. Put pool back to epoch prep state (new contract)
 */

class RecoverLiveEpochBoxSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {
  /*
  In Oracle Pool v7, the epochPrepScript has two spending paths:
  1. poolAction
  2. updateAction

  This class tests the updateAction
   */
  val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"
  val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyTokenId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyScript = "sigmaProp(true)"

  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))

  val fee = 1500000

  property("Recover broken update") {
    ergoClient.execute { implicit ctx: BlockchainContext =>
      val poolBoxValue = 1000000000L

      val oldOraclePool = new OraclePoolLive {}
      val newOraclePool = new OraclePoolLive {
        override def livePeriod = 12 // blocks          CHANGED!!
        override def prepPeriod = 6 // blocks           CHANGED!!
        override def buffer = 4 // blocks               CHANGED!!
      }

      // check that epoch prep address is different
      require(oldOraclePool.epochPrepAddress != newOraclePool.epochPrepAddress)

      // check that live epoch address is different
      require(oldOraclePool.liveEpochAddress != newOraclePool.liveEpochAddress)

      // datapoint script should not change
      require(oldOraclePool.dataPointAddress == newOraclePool.dataPointAddress)
      // dummy custom input box for funding various transactions
      val dummyFundingBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(10000000000L)
        .registers(KioskCollByte(Array(1)).getErgoValue)
        .tokens(new ErgoToken(dummyTokenId, 1))
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .build()
        .convertToInputWith(dummyTxId, 0)

      // old pool box
      val oldEpochPrepBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(poolBoxValue)
        .tokens(
          new ErgoToken(oldOraclePool.poolNFT, 1)
        )
        .contract(ctx.newContract(ScalaErgoConverters.getAddressFromString(oldOraclePool.epochPrepAddress).script))
        .registers(KioskLong(100).getErgoValue, KioskInt(100).getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 0)

      // new pool box
      val newEpochPrepBox = KioskBox(
        newOraclePool.epochPrepAddress,
        value = oldEpochPrepBox.getValue,
        registers = Array(KioskLong(100), KioskInt(100)),
        tokens = Array(oldOraclePool.poolNFT.encodeHex -> 1L)
      )

      val newEpochPrepScriptHash = KioskCollByte(Blake2b256(newOraclePool.epochPrepErgoTree.bytes))

      // old update box
      val updateBoxIn = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(10000000)
        .tokens(new ErgoToken(oldOraclePool.updateNFT, 1))
        .contract(ctx.newContract(ScalaErgoConverters.getAddressFromString(oldOraclePool.updateAddress).script))
        .registers(newEpochPrepScriptHash.getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 0)

      // new update box
      val updateBoxOut = KioskBox(
        oldOraclePool.updateAddress,
        value = updateBoxIn.getValue,
        registers = Array(newEpochPrepScriptHash),
        tokens = Array(oldOraclePool.updateNFT.encodeHex -> 1L)
      )

      val updateTx = TxUtil.createTx(
        inputBoxes = Array(oldEpochPrepBox, updateBoxIn, dummyFundingBox),
        dataInputs = Array(),
        boxesToCreate = Array(newEpochPrepBox, updateBoxOut),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )

      val newEpochPrepBoxCreated: InputBox = updateTx.getOutputsToSpend.get(0)

      val hash: Array[Byte] = Blake2b256(newOraclePool.epochPrepErgoTree.bytes)

      val endHeight = ctx.getHeight + newOraclePool.epochPeriod

      // start next epoch
      val newLiveEpochBox = KioskBox(
        newOraclePool.liveEpochAddress,
        value = newEpochPrepBoxCreated.getValue,
        registers = Array(KioskLong(100), KioskInt(endHeight), KioskCollByte(hash)),
        tokens = Array(oldOraclePool.poolNFT.encodeHex -> 1L)
      )

      val startLiveEpochTx = TxUtil.createTx(
        inputBoxes = Array(newEpochPrepBoxCreated, dummyFundingBox),
        dataInputs = Array(),
        boxesToCreate = Array(newLiveEpochBox),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )

      val liveEpochBox = startLiveEpochTx.getOutputsToSpend.get(0)

      // create oracle boxes
      lazy val addresses = Seq(
        "9eiuh5bJtw9oWDVcfJnwTm1EHfK5949MEm5DStc2sD1TLwDSrpx", // private key is 37cc5cb5b54f98f92faef749a53b5ce4e9921890d9fb902b4456957d50791bd0
        "9f9q6Hs7vXZSQwhbrptQZLkTx15ApjbEkQwWXJqD2NpaouiigJQ", // private key is 5878ae48fe2d26aa999ed44437cffd2d4ba1543788cff48d490419aef7fc149d
        "9fGp73EsRQMpFC7xaYD5JFy2abZeKCUffhDBNbQVtBtQyw61Vym", // private key is 3ffaffa96b2fd6542914d3953d05256cd505d4beb6174a2601a4e014c3b5a78e
        "9fSqnSHKLzRz7sRkfwNW4Rqtmig2bHNaaspsQms1gY2sU6LA2Ng", // private key is 148bb91ada6ad5e6b1bba02fe70ecd96095e00cbaf0f1f9294f02fedf9855ea0
        "9g3izpikC6xuvhnXxNHT1y5nwJNofMsoPiCgr4JXcZV6GUgWPqh" // private key is 148bb91ada6ad5e6b1bba02fe70ecd96095e00cbaf0f1f9294f02fedf9855ea1
      ).toArray

      val oracle0PrivateKey: scala.BigInt = scala.BigInt("37cc5cb5b54f98f92faef749a53b5ce4e9921890d9fb902b4456957d50791bd0", 16)
      val oracle1PrivateKey: scala.BigInt = scala.BigInt("5878ae48fe2d26aa999ed44437cffd2d4ba1543788cff48d490419aef7fc149d", 16)
      val oracle2PrivateKey: scala.BigInt = scala.BigInt("3ffaffa96b2fd6542914d3953d05256cd505d4beb6174a2601a4e014c3b5a78e", 16)
      val oracle3PrivateKey: scala.BigInt = scala.BigInt("148bb91ada6ad5e6b1bba02fe70ecd96095e00cbaf0f1f9294f02fedf9855ea0", 16)
      val oracle4PrivateKey: scala.BigInt = scala.BigInt("148bb91ada6ad5e6b1bba02fe70ecd96095e00cbaf0f1f9294f02fedf9855ea1", 16)
      import ScalaErgoConverters._
      val r4oracle0 = KioskGroupElement(stringToGroupElement(ErgoUtil.addressToGroupElement(addresses(0))))
      val r4oracle1 = KioskGroupElement(stringToGroupElement(ErgoUtil.addressToGroupElement(addresses(1))))
      val r4oracle2 = KioskGroupElement(stringToGroupElement(ErgoUtil.addressToGroupElement(addresses(2))))
      val r4oracle3 = KioskGroupElement(stringToGroupElement(ErgoUtil.addressToGroupElement(addresses(3))))
      val r4oracle4 = KioskGroupElement(stringToGroupElement(ErgoUtil.addressToGroupElement(addresses(4))))
      val r5oracle = KioskCollByte(Array(0x01))

      val oracle0Box = KioskBox(oldOraclePool.dataPointAddress, value = 200000000, registers = Array(r4oracle0, r5oracle), tokens = Array((oldOraclePool.oracleToken, 1)))
      val oracle1Box = KioskBox(oldOraclePool.dataPointAddress, value = 200000000, registers = Array(r4oracle1, r5oracle), tokens = Array((oldOraclePool.oracleToken, 1)))
      val oracle2Box = KioskBox(oldOraclePool.dataPointAddress, value = 200000000, registers = Array(r4oracle2, r5oracle), tokens = Array((oldOraclePool.oracleToken, 1)))
      val oracle3Box = KioskBox(oldOraclePool.dataPointAddress, value = 200000000, registers = Array(r4oracle3, r5oracle), tokens = Array((oldOraclePool.oracleToken, 1)))
      val oracle4Box = KioskBox(oldOraclePool.dataPointAddress, value = 200000000, registers = Array(r4oracle4, r5oracle), tokens = Array((oldOraclePool.oracleToken, 1)))

      val dummyOraclePoolTokenBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(10000000000L)
        .registers(KioskCollByte(Array(1)).getErgoValue)
        .tokens(new ErgoToken(oldOraclePool.oracleToken, 1))
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .build()
        .convertToInputWith(dummyTxId, 0)

      val bootStrapOracle0Tx =
        TxUtil.createTx(Array(dummyOraclePoolTokenBox), Array[InputBox](), Array(oracle0Box), fee, changeAddress, Array[String](), Array[DhtData](), false)
      val oracleBox0ToSpend = bootStrapOracle0Tx.getOutputsToSpend.get(0)

      val bootStrapOracle1Tx =
        TxUtil.createTx(Array(dummyOraclePoolTokenBox), Array[InputBox](), Array(oracle1Box), fee, changeAddress, Array[String](), Array[DhtData](), false)
      val oracleBox1ToSpend = bootStrapOracle1Tx.getOutputsToSpend.get(0)

      val bootStrapOracle2Tx =
        TxUtil.createTx(Array(dummyOraclePoolTokenBox), Array[InputBox](), Array(oracle2Box), fee, changeAddress, Array[String](), Array[DhtData](), false)
      val oracleBox2ToSpend = bootStrapOracle2Tx.getOutputsToSpend.get(0)

      val bootStrapOracle3Tx =
        TxUtil.createTx(Array(dummyOraclePoolTokenBox), Array[InputBox](), Array(oracle3Box), fee, changeAddress, Array[String](), Array[DhtData](), false)
      val oracleBox3ToSpend = bootStrapOracle3Tx.getOutputsToSpend.get(0)

      val bootStrapOracle4Tx =
        TxUtil.createTx(Array(dummyOraclePoolTokenBox), Array[InputBox](), Array(oracle4Box), fee, changeAddress, Array[String](), Array[DhtData](), false)
      val oracleBox4ToSpend = bootStrapOracle4Tx.getOutputsToSpend.get(0)

      val r5dataPoint = KioskCollByte(liveEpochBox.getId.getBytes)

      type DataPointBox = InputBox
      type DataPoint = KioskLong
      type Address = String
      type PrivateKey = BigInt

      // dataPoints to commit
      val r6dataPoint0: DataPoint = KioskLong(100000)
      val r6dataPoint1: DataPoint = KioskLong(100102)
      val r6dataPoint2: DataPoint = KioskLong(100103)
      val r6dataPoint3: DataPoint = KioskLong(100105)
      val r6dataPoint4: DataPoint = KioskLong(100107)

      val dataPointInfo3 = Array(
        (oracleBox1ToSpend, r4oracle1, r6dataPoint1, addresses(1), oracle1PrivateKey),
        (oracleBox2ToSpend, r4oracle2, r6dataPoint2, addresses(2), oracle2PrivateKey),
        (oracleBox3ToSpend, r4oracle3, r6dataPoint3, addresses(3), oracle3PrivateKey)
      )

      val dataPointInfo4 = Array(
        (oracleBox0ToSpend, r4oracle0, r6dataPoint0, addresses(0), oracle0PrivateKey),
        (oracleBox1ToSpend, r4oracle1, r6dataPoint1, addresses(1), oracle1PrivateKey),
        (oracleBox2ToSpend, r4oracle2, r6dataPoint2, addresses(2), oracle2PrivateKey),
        (oracleBox3ToSpend, r4oracle3, r6dataPoint3, addresses(3), oracle3PrivateKey)
      )

      val dataPointInfo5 = Array(
        (oracleBox0ToSpend, r4oracle0, r6dataPoint0, addresses(0), oracle0PrivateKey),
        (oracleBox1ToSpend, r4oracle1, r6dataPoint1, addresses(1), oracle1PrivateKey),
        (oracleBox2ToSpend, r4oracle2, r6dataPoint2, addresses(2), oracle2PrivateKey),
        (oracleBox3ToSpend, r4oracle3, r6dataPoint3, addresses(3), oracle3PrivateKey),
        (oracleBox4ToSpend, r4oracle4, r6dataPoint4, addresses(4), oracle4PrivateKey)
      )

      assert(liveEpochBox.getErgoTree.bytes.encodeHex == newOraclePool.liveEpochErgoTree.bytes.encodeHex)

      // collect three, four, five dataPoints
      the[Exception] thrownBy commitAndCollect(dataPointInfo3) should have message "Script reduced to false" // min data points is 4
      commitAndCollect(dataPointInfo4)
      commitAndCollect(dataPointInfo5)

      def commitDataPoint(dataPointBox: DataPointBox, r4dataPoint: KioskGroupElement, r6dataPoint: DataPoint, oraclePrivateKey: PrivateKey) = {
        val commitBoxToCreate = KioskBox(
          oldOraclePool.dataPointAddress,
          value = 200000000,
          registers = Array(r4dataPoint, r5dataPoint, r6dataPoint),
          tokens = Array((oldOraclePool.oracleToken, 1))
        )
        val createDataPointTx = TxUtil.createTx(
          Array(dataPointBox, dummyFundingBox),
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

      def collect(dataPointBoxes: Array[DataPointBox], dataPoints: Array[DataPoint], addresses: Array[Address], privateKey: PrivateKey) = {

        val privateKeys: Array[Option[PrivateKey]] = addresses.zipWithIndex.map {
          case (_, 0) => Some(privateKey)
          case _      => None
        }

        val tuples: Array[(DataPointBox, DataPoint, Address, Option[PrivateKey])] =
          (dataPointBoxes zip dataPoints) zip (addresses zip privateKeys) map {
            case ((dataPointBox, dataPoint), (address, optPrivateKey)) =>
              (dataPointBox, dataPoint, address, optPrivateKey)
          } sortBy (-_._2.value)

        val dataPointBoxesSorted: Array[DataPointBox] = tuples.map(_._1)

        val prepBoxToCreate = KioskBox(
          newOraclePool.epochPrepAddress,
          newLiveEpochBox.value - (dataPoints.length + 1) * newOraclePool.oracleReward,
          Array(
            KioskLong(dataPoints.map(_.value).sum / dataPoints.length),
            KioskInt(endHeight + newOraclePool.epochPeriod)
          ),
          newLiveEpochBox.tokens
        )

        var myIndex = 0

        val rewardBoxes: Array[KioskBox] = tuples.zipWithIndex.map {
          case ((_, _, address, Some(_)), i) =>
            myIndex = i
            KioskBox(address, newOraclePool.oracleReward * 2, Array(), Array())
          case ((_, _, address, _), _) =>
            KioskBox(address, newOraclePool.oracleReward, Array(), Array())
        }
        rewardBoxes(0) = rewardBoxes(0).copy(registers = Array(KioskInt(myIndex)))

        TxUtil.createTx(
          Array(liveEpochBox, dummyFundingBox),
          dataPointBoxesSorted,
          Array(prepBoxToCreate) ++ rewardBoxes,
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
