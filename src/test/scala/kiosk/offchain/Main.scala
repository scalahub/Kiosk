package kiosk.offchain

import play.api.libs.json.Json

object Main {
  implicit val dictionary = new Dictionary
  val parser = new Parser
  import parser._
  object Dummy {
    lazy val constantLong = Constant("myLong", DataType.Long, "1234L")
    lazy val constantInt = Constant("myInt", DataType.Long, "1234")
    lazy val constantCollByte = Constant("myCollByte", DataType.Long, "120a23")
    lazy val constantTokenRef = Constant("token_ref", DataType.Long, "120a23")

    lazy val binaryOp1 = BinaryOpResult("foo", "myLong", BinaryOp.Add, "token_ref")
    lazy val binaryOp2 = BinaryOpResult("bar", "foo", BinaryOp.Max, "myLong")
    lazy val binaryOp3 = BinaryOpResult("a", "foo", BinaryOp.Add, "bar")
    lazy val binaryOp4 = BinaryOpResult("b", "a", BinaryOp.Add, "foo")
    lazy val binaryOp5 = BinaryOpResult("c", "b", BinaryOp.Add, "a")

    lazy val unaryOp1 = UnaryOpResult("baz", "foo", UnaryOp.GExp)
    lazy val unaryOp2 = UnaryOpResult("abc", "baz", UnaryOp.Hash)

    lazy val constants = Some(Seq(constantLong, constantCollByte, constantInt, constantTokenRef))
    lazy val unaryOps = Some(Seq(unaryOp1, unaryOp2))
    lazy val binaryOps = Some(Seq(binaryOp1, binaryOp2, binaryOp3, binaryOp4, binaryOp5))

    lazy val inputRegister = RegisterDefiner(Some("inputRegister"), RegId.R4, DataType.CollByte, None, ref = Some("myLong"))

    lazy val dataInputRegister1 = RegisterDefiner(Some("dataInputRegister1"), RegId.R4, DataType.CollByte, None, ref = Some("c"))
    lazy val dataInputRegister2 = RegisterDefiner(Some("dataInputRegister2"), RegId.R4, DataType.CollByte, None, Some("inputRegister"))

    lazy val tokenQuantifier = Quantifier(QuantifierOp.MinimumTotal, Some(100), None)

    lazy val inputQuantifier = Quantifier(QuantifierOp.MaximumTotal, Some(10000), None)

    lazy val dataInputQuantifier = Quantifier(QuantifierOp.MaximumTotal, None, Some("foo"))

    lazy val inputToken = TokenDefiner(Some("inputToken"), Some(1), Some("id"), None, Some(tokenQuantifier))

    lazy val dataInputToken = TokenDefiner(Some("dataInputToken"), Some(1), None, Some("token_ref"), Some(tokenQuantifier))

    lazy val input = InputDefiner(Some("someInput"), Some("someId"), None, Some(Seq(inputToken)), Some(Seq(inputRegister)), Some(inputQuantifier))

    lazy val dataInput = InputDefiner(Some("someDataInput"), None, Some("someAddress"), Some(Seq(dataInputToken)), Some(Seq(dataInputRegister1, dataInputRegister2)), Some(dataInputQuantifier))

    lazy val protocol = Protocol(constants, binaryOps, unaryOps, Some(Seq(input)), Some(Seq(dataInput)), None, None)
  }

  def main(args: Array[String]): Unit = {
    import Dummy._
    val protocolToJson = Json.toJson(protocol)
    println(protocolToJson)
    dictionary.reset
    val jsonToProtocol = protocolToJson.as[Protocol]
    val str =
      """{"constants":[{"name":"myLong","type":"Long","value":"1234L"},{"name":"myCollByte","type":"Long","value":"120a23"},{"name":"myInt","type":"Long","value":"1234"},{"name":"token_ref","type":"Long","value":"120a23"}],"binaryOps":[{"name":"foo","left":"myLong","op":"Add","right":"token_ref"},{"name":"bar","left":"foo","op":"Max","right":"myLong"},{"name":"a","left":"foo","op":"Add","right":"bar"},{"name":"b","left":"a","op":"Add","right":"foo"},{"name":"c","left":"b","op":"Add","right":"a"}],"unaryOps":[{"name":"baz","input":"foo","op":"GExp"},{"name":"abc","input":"baz","op":"Hash"}],"inputs":[{"name":"someInput","id":"someId","tokens":[{"name":"inputToken","index":1,"id":"id","quantity":{"op":"MinimumTotal","amount":100}}],"registers":[{"name":"inputRegister","id":"R4","type":"CollByte","ref":"myLong"}],"nanoErgs":{"op":"MaximumTotal","amount":10000}}],"dataInputs":[{"name":"someDataInput","address":"someAddress","tokens":[{"name":"dataInputToken","index":1,"ref":"token_ref","quantity":{"op":"MinimumTotal","amount":100}}],"registers":[{"name":"dataInputRegister1","id":"R4","type":"CollByte","ref":"c"},{"name":"dataInputRegister2","id":"R4","type":"CollByte","ref":"inputRegister"}],"nanoErgs":{"op":"MaximumTotal","ref":"foo"}}]}""".stripMargin
    dictionary.reset
    val strToProtocol = Json.parse(str).as[Protocol]
    require(jsonToProtocol == protocol)
    require(strToProtocol == protocol)
  }
}
