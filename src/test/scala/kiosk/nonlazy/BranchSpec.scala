package kiosk.nonlazy

import kiosk.Box
import kiosk.ergo.{DhtData, KioskBox, KioskCollByte, KioskInt, KioskLong}
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, ErgoToken, HttpClientTesting, InputBox, SignedTransaction}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.util.Try

class BranchSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {

  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))
  val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"
  val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyScript = "{sigmaProp(1 < 2)}"

  property("Not-so-lazy evaluation") {
    ergoClient.execute { implicit ctx: BlockchainContext =>
      val height = ctx.getHeight
      val fee = 1500000

      val branchBoxToCreate = KioskBox(
        Branch.branchBoxAddress,
        value = 2000000,
        registers = Array(),
        tokens = Array()
      )

      // dummy custom input box for funding various transactions
      val customInputBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(10000000000000L)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .build()
        .convertToInputWith(dummyTxId, 0)

      val branchBoxCreationTx: SignedTransaction = Box.$createTx(
        inputBoxes = Array(customInputBox),
        dataInputs = Array[InputBox](),
        boxesToCreate = Array(branchBoxToCreate),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )

      val dataBoxWithLong = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(10000000L)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .registers(KioskLong(1L).getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 0)

      val branchBox = branchBoxCreationTx.getOutputsToSpend.get(0)

      val dataBoxWithCollByte = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(10000000L)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .registers(KioskCollByte("hello".getBytes()).getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 0)

      val longSelectionBox = KioskBox(
        changeAddress,
        value = 2000000,
        registers = Array(KioskInt(1)),
        tokens = Array()
      )

      val collByteSelectionBox = KioskBox(
        changeAddress,
        value = 2000000,
        registers = Array(KioskInt(2)),
        tokens = Array()
      )

      val tryLongBranchTx =
        Try(
          Box.$createTx(
            inputBoxes = Array(branchBox, customInputBox),
            dataInputs = Array[InputBox](dataBoxWithLong),
            boxesToCreate = Array(longSelectionBox),
            fee,
            changeAddress,
            Array[String](),
            Array[DhtData](),
            false
          )
        )

      val tryCollByteBranchTx =
        Try(
          Box.$createTx(
            inputBoxes = Array(branchBox, customInputBox),
            dataInputs = Array[InputBox](dataBoxWithCollByte),
            boxesToCreate = Array(collByteSelectionBox),
            fee,
            changeAddress,
            Array[String](),
            Array[DhtData](),
            false
          )
        )

      assert(tryLongBranchTx.isFailure)
      assert(tryCollByteBranchTx.isSuccess)
    }
  }
}
