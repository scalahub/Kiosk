package kiosk.multisecret

import kiosk.ergo._
import kiosk.{Box, ECC}
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, HttpClientTesting, InputBox}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import sigmastate.eval._
import sigmastate.interpreter.CryptoConstants
import special.sigma.GroupElement

class MultiProveDlogSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {
  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))

  val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"
  val dummyTxId1 = "d9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyTxId2 = "e9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyScript = "{sigmaProp(1 < 2)}"

  val secret1 = BigInt("1111111111111111111111111111111111111111111111111111111")
  val secret2 = BigInt("9999999999999999999999999999999999999999999999999")

  private val defaultGenerator: GroupElement = CryptoConstants.dlogGroup.generator
  private val gX: GroupElement = defaultGenerator.exp(secret1.bigInteger)
  private val gY: GroupElement = defaultGenerator.exp(secret2.bigInteger)
  /*
expected:
  Vector(
    ErgoBox(
      ce70d0abe6a3bfb3dcc33aa91d275ffb9f524d130c1053349f104e32396d4f1e,
      100000000,
      ErgoTree(0,WrappedArray(),Right(ConstantNode(SigmaProp(ProveDlog(ECPoint(cf33bd,17bd6c,...))),SSigmaProp)),0,[B@3bcd426c),
      tokens: (Coll()),
      f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809,
      0,
      Map(),
      123414
    ),
    ErgoBox(
      388236b2bb6323e6cc41aa1a185bfcfdcffe0625a05c7c47fe9674e7d9dcf167,
      100000000,
      ErgoTree(0,WrappedArray(),Right(ConstantNode(SigmaProp(ProveDlog(ECPoint(565453,2dac17,...))),SSigmaProp)),0,[B@c4c0b41),
      tokens: (Coll()),
      f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809,
      1,
      Map(),
      123414
    )
  ),

got
  ArrayBuffer(
    ErgoBox(
      ce70d0abe6a3bfb3dcc33aa91d275ffb9f524d130c1053349f104e32396d4f1e,
      100000000,
      ErgoTree(0,WrappedArray(),Right(ConstantNode(SigmaProp(ProveDlog(ECPoint(cf33bd,17bd6c,...))),SSigmaProp)),0,[B@3bcd426c),
      tokens: (Coll()),
      f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809,
      0,
      Map(),
      123414
    )
  )
   */
  property("Multi proveDlog") {
    ergoClient.execute { implicit ctx: BlockchainContext =>
      val fee = 1500000
      val contract1 = ctx.compileContract(
        ConstantsBuilder
          .create()
          .item(
            "gX",
            gX
          )
          .build(),
        "proveDlog(gX)"
      )
      val contract2 = ctx.compileContract(
        ConstantsBuilder
          .create()
          .item(
            "gY",
            gY
          )
          .build(),
        "proveDlog(gY)"
      )

      val box1 = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(100000000L)
        .contract(contract1)
        .build()
        .convertToInputWith(dummyTxId1, 0)

      val box2 = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(101500000L)
        .contract(contract1)
        .build()
        .convertToInputWith(dummyTxId2, 0)

      val dummyOutput = KioskBox(
        changeAddress,
        value = 200000000L,
        registers = Array(),
        tokens = Array()
      )

      Box.$createTx(
        inputBoxes = Array(box1, box2),
        dataInputs = Array[InputBox](),
        boxesToCreate = Array(dummyOutput),
        fee,
        changeAddress,
        Array[String](secret1.toString(10), secret2.toString(10)),
        Array[DhtData](),
        false
      )

    }
  }
}
