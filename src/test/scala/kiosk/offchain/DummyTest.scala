package kiosk.offchain

import kiosk.offchain.model.BinaryOperator.Sub
import kiosk.offchain.model.{Address, BinaryOp, Constant, DataType, FilterOp, Id, Input, Output, Protocol, RegNum, Register, Token}
import kiosk.offchain.parser.Parser
import play.api.libs.json.JsValue

object DummyTest {
  // constants
  val myInt = Constant("myInt", DataType.Int, "0")
  val myBoxId = Constant("myBoxId", DataType.CollByte, "b9e5c470e392ee8290c90c607128f50b5e4bf0d87b9e5e40f685a13e94d129c8")
  val ten = Constant("ten", DataType.Long, "10")
  val emissionBoxAddress = Constant(
    "emissionBoxAddress",
    DataType.Address,
    "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi"
  )
  val timeStampTokenId = Constant("timeStampTokenId", DataType.CollByte, "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")
  val minTimeStampTokenAmount = Constant("minTimeStampTokenAmount", DataType.Long, "100")

  val foo = BinaryOp("foo", "timeStampTokenAmount1", Sub, "ten")
  // declarations
  val dataInputBoxId: Option[Id] = Some(Id(name = None, value = Some("myBoxId")))
  val dataInput = Input(dataInputBoxId, address = None, registers = None, tokens = None, nanoErgs = None)

  val inputBoxAddress = Address(name = None, value = Some("emissionBoxAddress"))
  val inputBoxTokenId = Id(name = None, value = Some("timeStampTokenId"))
  val inputBoxTokenAmount1 = model.Long(name = Some("timeStampTokenAmount1"), value = None, filter = None)
  //  val inputBoxTokenAmount2 = model.Long(name = Some("timeStampTokenAmount2"), value = Some("minTimeStampTokenAmount"), filter = Some(FilterOp.Ge))
  //  val inputBoxTokenAmount2 = model.Long(name = Some("timeStampTokenAmount2"), value = Some("minTimeStampTokenAmount"), filter = None)
  val inputBoxTokenAmount2 = model.Long(name = Some("timeStampTokenAmount2"), value = Some("timeStampTokenAmount1"), filter = None)
  val inputBoxTokenAmount3 = model.Long(name = None, value = Some("foo"), filter = Some(FilterOp.Gt))
  val inputBoxToken1 = Token(index = 0, tokenId = inputBoxTokenId, amount = inputBoxTokenAmount1)
  val inputBoxToken2 = Token(index = 0, tokenId = inputBoxTokenId, amount = inputBoxTokenAmount2)
  val inputBoxToken3 = Token(index = 0, tokenId = inputBoxTokenId, amount = inputBoxTokenAmount3)
  val register = Register(name = None, value = Some("HEIGHT"), num = RegNum.R4, `type` = DataType.Int)
  val input1 = Input(boxId = None, address = Some(inputBoxAddress), registers = None, tokens = Some(Seq(inputBoxToken1)), nanoErgs = None)
  val input2 = Input(boxId = None, address = Some(inputBoxAddress), registers = None, tokens = Some(Seq(inputBoxToken2)), nanoErgs = None)
  val input3 = Input(boxId = None, address = Some(inputBoxAddress), registers = None, tokens = Some(Seq(inputBoxToken3)), nanoErgs = None)

  val output = Output(inputBoxAddress, Some(Seq(register)), tokens = None, nanoErgs = model.Long(name = None, value = Some("timeStampTokenAmount1"), filter = None))
  val constants: Option[Seq[Constant]] = Some(Seq(myBoxId, emissionBoxAddress, timeStampTokenId, minTimeStampTokenAmount, ten))
  val dataInputs = Some(Seq(dataInput))
  val inputs = Seq(input1, input2, input3)
  def main(args: Array[String]): Unit = {
    val protocol = Protocol(constants, dataInputs, inputs, outputs = Seq(output), fee = None, binaryOps = Some(Seq(foo)), unaryOps = None, conversions = None)
    val json: JsValue = Parser.unparse(protocol)
    val string = json.toString()
    //    println(string)
    val json2Protocol = Parser.parse(string)
    println(Parser.unparse(json2Protocol))
    compiler.Compiler.compile(json2Protocol)
  }

}
