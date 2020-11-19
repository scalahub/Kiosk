package kiosk.offchain

import kiosk.offchain.model.BinaryOperator.Sub
import kiosk.offchain.model._
import kiosk.offchain.parser.Parser
import play.api.libs.json.JsValue

object TimestampProtocol {
  // constants
  val boxIdToTimestamp = Constant("boxIdToTimestamp", DataType.CollByte, "b9e5c470e392ee8290c90c607128f50b5e4bf0d87b9e5e40f685a13e94d129c8")
  val one = Constant("one", DataType.Long, "1")
  val minStorageRent = Constant("minStorageRent", DataType.Long, "2000000")
  val emissionAddress = Constant(
    "emissionAddress",
    DataType.Address,
    "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi"
  )
  val timestampAddress = Constant("timestampAddress", DataType.Address, "4MQyMKvMbnCJG3aJ")
  val tokenId = Constant("tokenId", DataType.CollByte, "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")
  val minTokenAmount = Constant("minTokenAmount", DataType.Long, "2")

  // operations
  val balanceTokenAmount = BinaryOp("balanceTokenAmount", "inputTokenAmount", Sub, "one")

  // declarations
  val dataInputBoxId: Option[Id] = Some(Id(name = None, value = Some("boxIdToTimestamp")))
  val dataInput = Input(dataInputBoxId, address = None, registers = None, tokens = None, nanoErgs = None)

  val emission = Address(name = None, value = Some("emissionAddress"))
  val timestamp = Address(name = None, value = Some("timestampAddress"))
  val timestampTokenId = Id(name = None, value = Some("tokenId"))

  val inputTokenAmount = model.Long(name = Some("inputTokenAmount"), value = Some("minTokenAmount"), filter = Some(FilterOp.Ge))
  val outputTokenAmount = model.Long(name = None, value = Some("balanceTokenAmount"), filter = None)
  val timestampTokenAmount = model.Long(name = None, value = Some("one"), filter = None)

  val inputToken = Token(index = 0, tokenId = timestampTokenId, amount = inputTokenAmount)
  val outputToken = Token(index = 0, tokenId = timestampTokenId, amount = outputTokenAmount)
  val timestampToken = Token(index = 0, tokenId = timestampTokenId, amount = timestampTokenAmount)
  val register1 = Register(name = None, value = Some("boxIdToTimestamp"), num = RegNum.R4, `type` = DataType.CollByte)
  val register2 = Register(name = None, value = Some("HEIGHT"), num = RegNum.R5, `type` = DataType.Int)

  val inputNanoErgs = Long(name = Some("inputNanoErgs"), value = None, filter = None)
  val outputNanoErgs = model.Long(name = None, value = Some("inputNanoErgs"), filter = None)
  val timestampNanoErgs = model.Long(name = None, value = Some("minStorageRent"), filter = None)

  val input = Input(
    boxId = None,
    address = Some(emission),
    registers = None,
    tokens = Some(Seq(inputToken)),
    nanoErgs = Some(inputNanoErgs)
  )

  val output1 = Output(emission, None, tokens = Some(Seq(outputToken)), nanoErgs = outputNanoErgs)
  val output2 = Output(timestamp, Some(Seq(register1, register2)), tokens = Some(Seq(timestampToken)), nanoErgs = timestampNanoErgs)

  val constants: Option[Seq[Constant]] = Some(Seq(boxIdToTimestamp, emissionAddress, timestampAddress, tokenId, minTokenAmount, one, minStorageRent))
  val dataInputs = Some(Seq(dataInput))
  val inputs = Seq(input)
  def main(args: Array[String]): Unit = {
    val protocol = Protocol(constants, dataInputs, inputs, outputs = Seq(output1, output2), fee = None, binaryOps = Some(Seq(balanceTokenAmount)), unaryOps = None, conversions = None)
    val json: JsValue = Parser.unparse(protocol)
    val string = json.toString()
    val json2Protocol = Parser.parse(string)
    println(Parser.unparse(json2Protocol))
    compiler.Compiler.compile(json2Protocol)
  }

}
