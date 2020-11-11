package kiosk.offchain

import kiosk.offchain
import kiosk.offchain.model._
import kiosk.offchain.parser.Parser
import kiosk.offchain.compiler._

object Main {
  object Dummy {
    lazy val myLong1 = Constant("myLong1", DataType.Long, "1234L")
    lazy val myInt = Constant("myInt", DataType.Long, "1234")
    lazy val myCollByte = Constant("myCollByte", DataType.CollByte, "123abc")
    lazy val myTokenId = Constant("myTokenId", DataType.CollByte, "123abc")
    lazy val myGroupElement = Constant("myGroupElement", DataType.GroupElement, "123abc")
    lazy val myErgoTree11 = Constant("myErgoTree11", DataType.ErgoTree, "123abc")

    lazy val myLong2 = BinaryOp("myLong2", "myLong1", BinaryOperator.Add, "myInt")
    lazy val myLong3 = BinaryOp("myLong3", "myLong2", BinaryOperator.Max, "myLong1")
    lazy val myLong4 = BinaryOp("myLong4", "myLong2", BinaryOperator.Add, "myLong3")
    lazy val myLong5 = BinaryOp("myLong5", "myLong4", BinaryOperator.Add, "myLong2")
    lazy val myLong6 = BinaryOp("myLong6", "myLong5", BinaryOperator.Add, "myLong4")

    lazy val myLong7 = UnaryOp("myLong7", "myLong2", UnaryOperator.Sum)
    lazy val myLong8 = UnaryOp("myLong8", "myLong7", UnaryOperator.Sum)

    lazy val myErgoTree1 = Conversion("myErgoTree1", "myGroupElement", UnaryConverter.ProveDlog)
    lazy val myCollByte2 = Conversion("myCollByte2", "myErgoTree1", UnaryConverter.ToCollByte)

    lazy val constants = Some(Seq(myLong1, myCollByte, myInt, myTokenId, myGroupElement))
    lazy val unaryOps = Some(Seq(myLong7, myLong8))
    lazy val unaryConverters = Some(Seq(myErgoTree1, myCollByte2))
    lazy val binaryOps = Some(Seq(myLong2, myLong3, myLong4, myLong5, myLong6))

    lazy val myRegister1 = Register(Some("myRegister1"), RegNum.R4, DataType.CollByte, None, ref = Some("myTokenId"))
    lazy val myRegister2 = Register(Some("myRegister2"), RegNum.R4, DataType.CollByte, None, ref = Some("myCollByte"))
    lazy val myRegister3 = Register(Some("myRegister3"), RegNum.R4, DataType.CollByte, None, ref = Some("myRegister1"))
    lazy val myRegister4 = Register(Some("myRegister4"), RegNum.R4, DataType.CollByte, None, ref = Some("myRegister2"))

    lazy val myQuantifier1 = RangeFilter(QuantifierOp.Ge, Some(100), None)
    lazy val myQuantifier2 = RangeFilter(QuantifierOp.Ge, Some(10000), None)
    lazy val myQuantifier3 = RangeFilter(QuantifierOp.Le, None, Some("myLong2"))

    lazy val myToken1 = Token(
      index = Some(1),
      id = Some(CollByte(name = None, value = Some("123abc"), ref = None)),
      numTokens = Some(
        model.Long(
          name = Some("someLong1"),
          value = None,
          ref = None,
          filters = Some(Seq(myQuantifier1))
        ))
    )

    lazy val myToken2 = Token(
      index = Some(1),
      id = Some(CollByte(name = None, value = Some("123abc"), ref = None)),
      numTokens = Some(
        model.Long(
          name = Some("someLong2"),
          value = None,
          ref = None,
          filters = Some(Seq(myQuantifier1))
        ))
    )

    lazy val myToken3 = Token(
      index = Some(1),
      id = None,
      numTokens = Some(
        model.Long(name = Some("someLong3"), value = None, ref = None, filters = Some(Seq(myQuantifier1)))
      )
    )

    lazy val myInput1 = Input(
      boxId = Some(CollByte(name = None, value = Some("someId"), ref = None)),
      address = None,
      registers = Some(Seq(myRegister3)),
      tokens = Some(Seq(myToken1)),
      nanoErgs = Some(model.Long(name = None, value = None, ref = None, filters = Some(Seq(myQuantifier2)))),
      None
    )

    lazy val myInput2 = Input(
      None,
      Some(Address(name = None, value = Some("someId"), ref = None)),
      registers = Some(Seq(myRegister4)),
      tokens = Some(Seq(myToken2)),
      nanoErgs = Some(model.Long(name = None, value = None, ref = None, filters = Some(Seq(myQuantifier2)))),
      Some(model.Long(name = Some("someLong4"), value = None, ref = None, filters = Some(Seq(myQuantifier1))))
    )

    lazy val myInput3 = Input(
      boxId = None,
      address = Some(Address(name = None, value = Some("someAddress"), ref = None)),
      registers = Some(Seq(myRegister1, myRegister2)),
      tokens = Some(Seq(myToken3)),
      nanoErgs = Some(model.Long(name = None, value = None, ref = None, filters = Some(Seq(myQuantifier3)))),
      None
    )

    lazy val protocol = Protocol(
      constants,
      binaryOps,
      unaryOps,
      unaryConverters,
      dataInputs = Some(Seq(myInput3, myInput2)),
      inputs = Some(Seq(myInput1)),
      outputs = None,
      None
    )
  }

  def main(args: Array[String]): Unit = {
    import Dummy._

    val protocolToJson = Parser.unparse(protocol)
    val protocolToJsonToProtocol = Parser.parse(protocolToJson.toString())
    println(protocolToJson)
    val str =
      """{"constants":[{"val":"myLong1","type":"Long","value":"1234L"},{"val":"myCollByte","type":"CollByte","value":"123abc"},{"val":"myInt","type":"Long","value":"1234"},{"val":"myTokenId","type":"CollByte","value":"123abc"},{"val":"myGroupElement","type":"GroupElement","value":"123abc"}],"binaryOps":[{"val":"myLong2","left":"myLong1","op":"Add","right":"myInt"},{"val":"myLong3","left":"myLong2","op":"Max","right":"myLong1"},{"val":"myLong4","left":"myLong2","op":"Add","right":"myLong3"},{"val":"myLong5","left":"myLong4","op":"Add","right":"myLong2"},{"val":"myLong6","left":"myLong5","op":"Add","right":"myLong4"}],"unaryOps":[{"val":"myLong7","operand":"myLong2","op":"Sum"},{"val":"myLong8","operand":"myLong7","op":"Sum"}],"conversions":[{"to":"myErgoTree1","from":"myGroupElement","converter":"ProveDlog"},{"to":"myCollByte2","from":"myErgoTree1","converter":"ToCollByte"}],"dataInputs":[{"address":{"value":"someAddress"},"registers":[{"name":"myRegister1","num":"R4","type":"CollByte","ref":"myTokenId"},{"name":"myRegister2","num":"R4","type":"CollByte","ref":"myCollByte"}],"tokens":[{"index":1,"numTokens":{"name":"someLong3","filters":[{"op":"Ge","value":100}]}}],"nanoErgs":{"filters":[{"op":"Le","ref":"myLong2"}]}},{"address":{"value":"someId"},"registers":[{"name":"myRegister4","num":"R4","type":"CollByte","ref":"myRegister2"}],"tokens":[{"index":1,"id":{"value":"123abc"},"numTokens":{"name":"someLong2","filters":[{"op":"Ge","value":100}]}}],"nanoErgs":{"filters":[{"op":"Ge","value":10000}]},"boxCount":{"name":"someLong4","filters":[{"op":"Ge","value":100}]}}],"inputs":[{"boxId":{"value":"someId"},"registers":[{"name":"myRegister3","num":"R4","type":"CollByte","ref":"myRegister1"}],"tokens":[{"index":1,"id":{"value":"123abc"},"numTokens":{"name":"someLong1","filters":[{"op":"Ge","value":100}]}}],"nanoErgs":{"filters":[{"op":"Ge","value":10000}]}}]}
        |""".stripMargin
    val strToProtocol = Parser.parse(str)
    offchain.compiler.Compiler.compile(protocol)
    require(protocolToJsonToProtocol == protocol, "protocol")
    require(strToProtocol == protocol)
  }
}
