package kiosk.offchain

import kiosk.offchain.model.BinaryOperator.Sub
import kiosk.offchain.model._
import kiosk.offchain.parser.Parser
import play.api.libs.json.JsValue

object TimestampTest {
  val timestampScript =
    """{
        |"constants":[
        |   {"name":"myBoxId","type":"CollByte","value":"18d4ffec7a74662125340f3ed14da7d8af8ffd0624f9e2c69188de46c795c6e7"},
        |   {"name":"emissionAddress","type":"Address","value":"2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi"},
        |   {"name":"timestampAddress","type":"Address","value":"4MQyMKvMbnCJG3aJ"},
        |   {"name":"myTokenId","type":"CollByte","value":"dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"},
        |   {"name":"minTokenAmount","type":"Long","value":"2"},
        |   {"name":"one","type":"Long","value":"1"},
        |   {"name":"minStorageRent","type":"Long","value":"2000000"}
        |],
        |"dataInputs":[
        |   {"id":{"value":"myBoxId"}}
        |],
        |"inputs":[
        |   {
        |     "address":{"value":"emissionAddress"},
        |     "tokens":[
        |        {
        |           "index":0,
        |           "id":{"value":"myTokenId"},
        |           "amount":{"name":"inputTokenAmount","value":"minTokenAmount","filter":"Ge"}
        |        }
        |     ],
        |     "nanoErgs":{"name":"inputNanoErgs"}
        |   }
        |],
        |"outputs":[
        |   {
        |      "address":{"value":"emissionAddress"},
        |      "tokens":[
        |         {
        |            "index":0,
        |            "id":{"value":"myTokenId"},
        |            "amount":{"value":"balanceTokenAmount"}
        |         }
        |      ],
        |      "nanoErgs":{"value":"inputNanoErgs"}
        |   },
        |   {
        |      "address":{"value":"timestampAddress"},
        |      "registers":[
        |         {
        |            "value":"myBoxId",
        |            "num":"R4",
        |            "type":"CollByte"
        |         },
        |         {
        |            "value":"HEIGHT",
        |            "num":"R5",
        |            "type":"Int"
        |         }
        |      ],
        |      "tokens":[
        |         {
        |            "index":0,
        |            "id":{"value":"myTokenId"},
        |            "amount":{"value":"one"}
        |         }
        |      ],
        |      "nanoErgs":{"value":"minStorageRent"}
        |   }
        |],
        |"binaryOps":[
        |   {"name":"balanceTokenAmount","first":"inputTokenAmount","op":"Sub","second":"one"}
        |]
        |}
        |""".stripMargin
  // constants
  val myBoxId = Constant("myBoxId", DataType.CollByte, "18d4ffec7a74662125340f3ed14da7d8af8ffd0624f9e2c69188de46c795c6e7")
  val one = Constant("one", DataType.Long, "1")
  val minStorageRent = Constant("minStorageRent", DataType.Long, "2000000")
  val emissionAddress = Constant(
    "emissionAddress",
    DataType.Address,
    "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi"
  )
  val timestampAddress = Constant("timestampAddress", DataType.Address, "4MQyMKvMbnCJG3aJ")
  val myTokenId = Constant("myTokenId", DataType.CollByte, "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")
  val minTokenAmount = Constant("minTokenAmount", DataType.Long, "2")

  // operations
  val balanceTokenAmount = BinaryOp("balanceTokenAmount", "inputTokenAmount", Sub, "one")

  // declarations
  val dataInputBoxId: Option[Id] = Some(Id(name = None, value = Some("myBoxId")))
  val dataInput = Input(dataInputBoxId, address = None, registers = None, tokens = None, nanoErgs = None)

  val emission = Address(name = None, value = Some("emissionAddress"))
  val timestamp = Address(name = None, value = Some("timestampAddress"))
  val timestampTokenId = Id(name = None, value = Some("myTokenId"))

  val inputTokenAmount = model.Long(name = Some("inputTokenAmount"), value = Some("minTokenAmount"), filter = Some(FilterOp.Ge))
  val outputTokenAmount = model.Long(name = None, value = Some("balanceTokenAmount"), filter = None)
  val timestampTokenAmount = model.Long(name = None, value = Some("one"), filter = None)

  val inputToken = Token(index = 0, id = timestampTokenId, amount = inputTokenAmount)
  val outputToken = Token(index = 0, id = timestampTokenId, amount = outputTokenAmount)
  val timestampToken = Token(index = 0, id = timestampTokenId, amount = timestampTokenAmount)
  val register1 = Register(name = None, value = Some("myBoxId"), num = RegNum.R4, `type` = DataType.CollByte)
  val register2 = Register(name = None, value = Some("HEIGHT"), num = RegNum.R5, `type` = DataType.Int)

  val inputNanoErgs = Long(name = Some("inputNanoErgs"), value = None, filter = None)
  val outputNanoErgs = model.Long(name = None, value = Some("inputNanoErgs"), filter = None)
  val timestampNanoErgs = model.Long(name = None, value = Some("minStorageRent"), filter = None)

  val input = Input(
    id = None,
    address = Some(emission),
    registers = None,
    tokens = Some(Seq(inputToken)),
    nanoErgs = Some(inputNanoErgs)
  )

  val output1 = Output(emission, None, tokens = Some(Seq(outputToken)), nanoErgs = outputNanoErgs)
  val output2 = Output(timestamp, Some(Seq(register1, register2)), tokens = Some(Seq(timestampToken)), nanoErgs = timestampNanoErgs)

  val constants: Option[Seq[Constant]] = Some(Seq(myBoxId, emissionAddress, timestampAddress, myTokenId, minTokenAmount, one, minStorageRent))
  val dataInputs = Some(Seq(dataInput))
  val inputs = Seq(input)
  def main(args: Array[String]): Unit = {
    val protocol = Protocol(constants, dataInputs, inputs, outputs = Seq(output1, output2), fee = None, binaryOps = Some(Seq(balanceTokenAmount)), unaryOps = None, conversions = None)
    val json: JsValue = Parser.unparse(protocol)
    val json2Protocol = Parser.parse(json.toString())
    val str2Protocol = Parser.parse(timestampScript)
    assert(str2Protocol == json2Protocol)
    println(Parser.unparse(json2Protocol))
    compiler.Compiler.compile(json2Protocol)
  }
}
