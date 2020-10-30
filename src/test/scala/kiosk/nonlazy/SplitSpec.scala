package kiosk.nonlazy

import kiosk.Box
import kiosk.ergo.{DhtData, KioskBox, KioskCollByte, KioskInt, KioskLong}
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, ErgoToken, HttpClientTesting, InputBox, SignedTransaction}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.util.Try

class SplitSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {

  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))
  val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"
  val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyScript = "{sigmaProp(1 < 2)}"

  property("Not-so-lazy evaluation") {
    ergoClient.execute { implicit ctx: BlockchainContext =>
      val fee = 1500000

      val splitBoxToCreate = KioskBox(
        Split.splitAddress,
        value = 2000000,
        registers = Array(),
        tokens = Array()
      )

      val leftBranchBoxToCreate = KioskBox(
        Split.leftAddress,
        value = 2000000,
        registers = Array(),
        tokens = Array()
      )

      val rightBranchBoxToCreate = KioskBox(
        Split.rightAddress,
        value = 2000000,
        registers = Array(),
        tokens = Array()
      )

      // dummy custom input box for funding various transactions
      val customInputBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(20000000L)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .build()
        .convertToInputWith(dummyTxId, 0)

      val splitBoxCreationTx: SignedTransaction = Box.$createTx(
        inputBoxes = Array(customInputBox),
        dataInputs = Array[InputBox](),
        boxesToCreate = Array(splitBoxToCreate, leftBranchBoxToCreate, rightBranchBoxToCreate),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )

      val splitBox = splitBoxCreationTx.getOutputsToSpend.get(0)
      val leftBranchBox = splitBoxCreationTx.getOutputsToSpend.get(1)
      val rightBranchBox = splitBoxCreationTx.getOutputsToSpend.get(2)

      val leftSelectionBox = KioskBox(
        changeAddress,
        value = 2000000,
        registers = Array(KioskInt(1)),
        tokens = Array()
      )

      val rightSelectionBox = KioskBox(
        changeAddress,
        value = 2000000,
        registers = Array(KioskInt(2)),
        tokens = Array()
      )

      val dataBoxWithLong = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(1000000L)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .registers(KioskLong(1L).getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 0)

      val dataBoxWithCollByte = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(1000000L)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .registers(KioskCollByte("hello".getBytes()).getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 0)

      Box.$createTx(
        inputBoxes = Array(splitBox, leftBranchBox),
        dataInputs = Array[InputBox](dataBoxWithLong),
        boxesToCreate = Array(leftSelectionBox),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )

      Box.$createTx(
        inputBoxes = Array(splitBox, rightBranchBox),
        dataInputs = Array[InputBox](dataBoxWithCollByte),
        boxesToCreate = Array(rightSelectionBox),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )
    }
  }
}
