package kiosk.offchain

import play.api.libs.json.Json

object Main {
  implicit val dictionary = new Dictionary
  val parser = new Parser
  import parser._
  object Dummy {
    lazy val constantLong = Constant("myLong", DataType.Long, "1234L")
    lazy val constantInt = Constant("myInt", DataType.Long, "1234")
    lazy val constantCollByte = Constant("myCollByte", DataType.CollByte, "120a23")
    lazy val constantTokenRef = Constant("token_ref", DataType.CollByte, "120a23")

    lazy val binaryOp1 = BinaryOpResult("foo", "myLong", BinaryOp.Add, "myInt")
    lazy val binaryOp2 = BinaryOpResult("bar", "foo", BinaryOp.Max, "myLong")
    lazy val binaryOp3 = BinaryOpResult("a", "foo", BinaryOp.Add, "bar")
    lazy val binaryOp4 = BinaryOpResult("b", "a", BinaryOp.Add, "foo")
    lazy val binaryOp5 = BinaryOpResult("c", "b", BinaryOp.Add, "a")

    lazy val unaryOp1 = UnaryOpResult("baz", "foo", UnaryOp.Sum)
    lazy val unaryOp2 = UnaryOpResult("abc", "baz", UnaryOp.Sum)

    lazy val unaryConverter1 = UnaryConverterResult("baz1", "foo", UnaryConverter.ProveDlog)
    lazy val unaryConverter2 = UnaryConverterResult("abc1", "baz", UnaryConverter.ToCollByte)

    lazy val constants = Some(Seq(constantLong, constantCollByte, constantInt, constantTokenRef))
    lazy val unaryOps = Some(Seq(unaryOp1, unaryOp2))
    lazy val unaryConverters = Some(Seq(unaryConverter1, unaryConverter2))
    lazy val binaryOps = Some(Seq(binaryOp1, binaryOp2, binaryOp3, binaryOp4, binaryOp5))

    lazy val dataInputRegister1 = Register(Some("dataInputRegister1"), RegNum.R4, DataType.CollByte, None, ref = Some("token_ref"))
    lazy val dataInputRegister2 = Register(Some("dataInputRegister2"), RegNum.R4, DataType.CollByte, None, ref = Some("myCollByte"))

    lazy val inputRegister = Register(Some("inputRegister"), RegNum.R4, DataType.CollByte, None, ref = Some("dataInputRegister2"))

    lazy val tokenQuantifier = RangeFilter(QuantifierOp.Ge, Some(100), None)

    lazy val inputQuantifier = RangeFilter(QuantifierOp.Ge, Some(10000), None)

    lazy val dataInputQuantifier = RangeFilter(QuantifierOp.Le, None, Some("foo"))

    lazy val inputToken = Token(
      index = Some(1),
      id = Some(CollByte(name = None, value = Some("id"), ref = None)),
      numTokens = Some(
        Long(
          name = Some("a1"),
          value = None,
          ref = None,
          filters = Some(Seq(tokenQuantifier))
        ))
    )

    lazy val dataInputToken = Token(
      index = Some(1),
      id = None,
      numTokens = Some(
        Long(name = Some("a2"), value = None, ref = None, filters = Some(Seq(tokenQuantifier)))
      )
    )

    lazy val input = SingleInput(
      boxId = Some(CollByte(name = None, value = Some("someId"), ref = None)),
      address = None,
      registers = Some(Seq(inputRegister)),
      tokens = Some(Seq(inputToken)),
      nanoErgs = Some(Long(name = None, value = None, ref = None, filters = Some(Seq(inputQuantifier))))
    )

    lazy val dataInput = SingleInput(
      boxId = None,
      address = Some(Address(name = None, value = Some("someAddress"), ref = None)),
      registers = Some(Seq(dataInputRegister1, dataInputRegister2)),
      tokens = Some(Seq(dataInputToken)),
      nanoErgs = Some(Long(name = None, value = None, ref = None, filters = Some(Seq(dataInputQuantifier))))
    )

    lazy val protocol = Protocol(constants, binaryOps, unaryOps, unaryConverters, Some(Seq(Left(dataInput))), Some(Seq(Left(input))), None, None)
  }

  def main(args: Array[String]): Unit = {
    import Dummy._
    val protocolToJson = Json.toJson(protocol)
    println(protocolToJson)
    dictionary.reset
    val jsonToProtocol = protocolToJson.as[Protocol]
    val str =
      """{"constants":[{"name":"myLong","type":"Long","value":"1234L"},{"name":"myCollByte","type":"CollByte","value":"120a23"},{"name":"myInt","type":"Long","value":"1234"},{"name":"token_ref","type":"CollByte","value":"120a23"}],"binaryOps":[{"name":"foo","left":"myLong","op":"Add","right":"myInt"},{"name":"bar","left":"foo","op":"Max","right":"myLong"},{"name":"a","left":"foo","op":"Add","right":"bar"},{"name":"b","left":"a","op":"Add","right":"foo"},{"name":"c","left":"b","op":"Add","right":"a"}],"unaryOps":[{"name":"baz","operand":"foo","op":"Sum"},{"name":"abc","operand":"baz","op":"Sum"}],"unaryConverters":[{"name":"baz1","operand":"foo","converter":"ProveDlog"},{"name":"abc1","operand":"baz","converter":"ToCollByte"}],"dataInputs":[{"address":{"value":"someAddress"},"registers":[{"name":"dataInputRegister1","num":"R4","type":"CollByte","ref":"token_ref"},{"name":"dataInputRegister2","num":"R4","type":"CollByte","ref":"myCollByte"}],"tokens":[{"index":1,"numTokens":{"name":"a2","filters":[{"op":"Ge","value":100}]}}],"nanoErgs":{"filters":[{"op":"Le","ref":"foo"}]}}],"inputs":[{"boxId":{"value":"someId"},"registers":[{"name":"inputRegister","num":"R4","type":"CollByte","ref":"dataInputRegister2"}],"tokens":[{"index":1,"id":{"value":"id"},"numTokens":{"name":"a1","filters":[{"op":"Ge","value":100}]}}],"nanoErgs":{"filters":[{"op":"Ge","value":10000}]}}]}
        |""".stripMargin
    dictionary.reset
    val strToProtocol = Json.parse(str).as[Protocol]
    require(jsonToProtocol == protocol)
    require(strToProtocol == protocol)
  }
}
