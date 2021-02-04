package kiosk.oraclepool.v5

import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo._
import kiosk.tx.TxUtil
import org.ergoplatform.appkit._
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scorex.crypto.hash.Blake2b256

import scala.util.Try

class VotingSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {
  /*
  In v0.2, the update box has two spending paths
  1. Update
  2. Voting

  This class tests the Voting path (the Update path is tested in UpdateSpec.scala)
   */
  val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"
  val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyScript = "sigmaProp(true)"

  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))

  val fee = 1500000

  property("Voting") {
    ergoClient.execute { implicit ctx: BlockchainContext =>
      val epochPool = new OraclePoolLive {}
      val newOraclePool = new OraclePoolLive {
        override lazy val maxNumOracles = 13
      }
      // dummy custom input box for funding various transactions
      val dummyFundingBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(fee)
        .registers(KioskCollByte(Array(1)).getErgoValue)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .build()
        .convertToInputWith(dummyTxId, 0)

      val updateBoxIn = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(epochPool.minStorageRent)
        .tokens(new ErgoToken(epochPool.updateNFT, 1))
        .contract(ctx.newContract(ScalaErgoConverters.getAddressFromString(epochPool.updateAddress).script))
        .registers(KioskCollByte(Array[Byte](1)).getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 0)

      val updatedEpochPrepScriptHash = KioskCollByte(Blake2b256(newOraclePool.epochPrepErgoTree.bytes))

      require(epochPool.epochPrepAddress != newOraclePool.epochPrepAddress)

      val updateBoxInBoxId = KioskCollByte(updateBoxIn.getId.getBytes)

      val updateBoxOut = KioskBox(
        epochPool.updateAddress,
        value = epochPool.minStorageRent,
        registers = Array(updatedEpochPrepScriptHash),
        tokens = Array(epochPool.updateNFT.encodeHex -> 1)
      )

      // ballot with 2 votes
      val ballot1 = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(epochPool.minStorageRent)
        .tokens(new ErgoToken(epochPool.ballotTokenId, 2))
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .registers(
          updatedEpochPrepScriptHash.getErgoValue,
          updateBoxInBoxId.getErgoValue
        )
        .build()
        .convertToInputWith(dummyTxId, 0)

      // ballot with 3 votes
      val ballot2 = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(epochPool.minStorageRent)
        .tokens(new ErgoToken(epochPool.ballotTokenId, 3))
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .registers(updatedEpochPrepScriptHash.getErgoValue, updateBoxInBoxId.getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 1)

      // ballot with 1 votes
      val ballot3 = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(epochPool.minStorageRent)
        .tokens(new ErgoToken(epochPool.ballotTokenId, 1))
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .registers(updatedEpochPrepScriptHash.getErgoValue, updateBoxInBoxId.getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 2)

      // Should succeed when having sufficient votes (6)
      TxUtil.createTx(
        inputBoxes = Array(updateBoxIn, dummyFundingBox),
        dataInputs = Array(ballot1, ballot2, ballot3),
        boxesToCreate = Array(updateBoxOut),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )

      // Should fail when having insufficient votes (5)
      assert(
        Try(
          TxUtil.createTx(
            inputBoxes = Array(updateBoxIn, dummyFundingBox),
            dataInputs = Array(ballot1, ballot2),
            boxesToCreate = Array(updateBoxOut),
            fee,
            changeAddress,
            Array[String](),
            Array[DhtData](),
            false
          )
        ).isFailure
      )

      // Should fail when old and new hashes are same
      val invalidUpdateBoxIn = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(epochPool.minStorageRent)
        .tokens(new ErgoToken(epochPool.updateNFT, 1))
        .contract(ctx.newContract(ScalaErgoConverters.getAddressFromString(epochPool.updateAddress).script))
        .registers(updatedEpochPrepScriptHash.getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 0)

      assert(
        Try(
          TxUtil.createTx(
            inputBoxes = Array(invalidUpdateBoxIn, dummyFundingBox),
            dataInputs = Array(ballot1, ballot2, ballot3),
            boxesToCreate = Array(updateBoxOut),
            fee,
            changeAddress,
            Array[String](),
            Array[DhtData](),
            false
          )
        ).isFailure
      )
    }
  }
}
