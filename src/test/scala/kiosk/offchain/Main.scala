package kiosk.offchain

import kiosk.offchain
import kiosk.offchain.model._
import kiosk.offchain.parser.Parser
import kiosk.offchain.compiler._

object Main {
  object Dummy {
    lazy val myLong1 = Constant("myLong1", DataType.Long, "1234L")
    lazy val myInt = Constant("myInt", DataType.Int, "1234")
    lazy val myCollByte = Constant("myCollByte", DataType.CollByte, "123abc")
    lazy val myTokenId = Constant("myTokenId", DataType.CollByte, "123abc")
    lazy val myGroupElement = Constant("myGroupElement", DataType.GroupElement, "123abc")
    lazy val myErgoTree1 = Constant("myErgoTree1", DataType.ErgoTree, "123abc")

    lazy val myIntToLong = Conversion("myIntToLong", "myInt", UnaryConverter.ToLong)
    lazy val myLong2 = BinaryOp("myLong2", "myLong1", BinaryOperator.Add, "myIntToLong")
    lazy val myLong3 = BinaryOp("myLong3", "myLong2", BinaryOperator.Max, "myLong1")
    lazy val myLong4 = BinaryOp("myLong4", "myLong2", BinaryOperator.Add, "myLong3")
    lazy val myLong5 = BinaryOp("myLong5", "myLong4", BinaryOperator.Add, "myLong2")
    lazy val myLong6 = BinaryOp("myLong6", "myLong5", BinaryOperator.Add, "myLong4")

    lazy val myLong7 = UnaryOp("myLong7", "myLong2", UnaryOperator.Sum)
    lazy val myLong8 = UnaryOp("myLong8", "myLong7", UnaryOperator.Sum)

    lazy val myErgoTree2 = Conversion("myErgoTree2", "myGroupElement", UnaryConverter.ProveDlog)
    lazy val myCollByte2 = Conversion("myCollByte2", "myErgoTree2", UnaryConverter.ToCollByte)
    lazy val myAddress = Conversion("myAddress", "myErgoTree1", UnaryConverter.ToAddress)

    lazy val constants = Some(Seq(myLong1, myCollByte, myInt, myTokenId, myGroupElement, myErgoTree1))
    lazy val unaryOps = Some(Seq(myLong7, myLong8))
    lazy val conversions = Some(Seq(myErgoTree2, myCollByte2, myAddress, myIntToLong))
    lazy val binaryOps = Some(Seq(myLong2, myLong3, myLong4, myLong5, myLong6))

    lazy val myRegister1 = Register(Some("myRegister1"), RegNum.R4, DataType.CollByte, None, ref = Some("myTokenId"))
    lazy val myRegister2 = Register(Some("myRegister2"), RegNum.R4, DataType.CollByte, None, ref = Some("myCollByte"))
    lazy val myRegister3 = Register(Some("myRegister3"), RegNum.R4, DataType.CollByte, None, ref = Some("myRegister1"))
    lazy val myRegister4 = Register(Some("myRegister4"), RegNum.R4, DataType.CollByte, None, ref = Some("myRegister2"))

    lazy val myToken1 = Token(
      index = Some(1),
      id = Some(CollByte(name = None, value = Some("123abc"), ref = None)),
      numTokens = Some(
        model.Long(
          name = Some("someLong1"),
          value = None,
          ref = None,
          op = Some(FilterOp.Le)
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
          op = Some(FilterOp.Gt)
        )
      )
    )

    lazy val myToken3 = Token(
      index = Some(1),
      id = None,
      numTokens = Some(
        model.Long(name = Some("someLong3"), value = None, ref = None, op = Some(FilterOp.Gt))
      )
    )

    lazy val myInput1 = Input(
      boxId = Some(CollByte(name = None, value = None, ref = Some("myCollByte"))),
      address = None,
      registers = Some(Seq(myRegister3)),
      tokens = Some(Seq(myToken1)),
      nanoErgs = Some(model.Long(name = None, value = None, ref = None, op = Some(FilterOp.Gt)))
    )

    lazy val myInput2 = Input(
      None,
      Some(Address(name = None, value = None, ref = Some("myAddress"))),
      registers = Some(Seq(myRegister4)),
      tokens = Some(Seq(myToken2)),
      nanoErgs = Some(model.Long(name = None, value = None, ref = None, op = Some(FilterOp.Gt)))
    )

    lazy val myInput3 = Input(
      boxId = None,
      address = Some(Address(name = None, value = Some("someAddress"), ref = None)),
      registers = Some(Seq(myRegister1, myRegister2)),
      tokens = Some(Seq(myToken3)),
      nanoErgs = Some(model.Long(name = None, value = None, ref = None, op = Some(FilterOp.Gt)))
    )

    lazy val protocol = Protocol(
      constants,
      binaryOps,
      unaryOps,
      conversions,
      dataInputs = Some(Seq(myInput3, myInput2)),
      inputs = Some(Seq(myInput1)),
      outputs = None,
      Some(Long(None, None, Some("myLong8"), None))
    )
  }

  def main(args: Array[String]): Unit = {
    import Dummy._

    val protocolToJson = Parser.unparse(protocol)
    val protocolToJsonToProtocol = Parser.parse(protocolToJson.toString())
    println(protocolToJson)
    val str =
      """{"constants":[{"val":"myLong1","type":"Long","value":"1234L"},{"val":"myCollByte","type":"CollByte","value":"123abc"},{"val":"myInt","type":"Int","value":"1234"},{"val":"myTokenId","type":"CollByte","value":"123abc"},{"val":"myGroupElement","type":"GroupElement","value":"123abc"},{"val":"myErgoTree1","type":"ErgoTree","value":"123abc"}],"binaryOps":[{"val":"myLong2","left":"myLong1","op":"Add","right":"myIntToLong"},{"val":"myLong3","left":"myLong2","op":"Max","right":"myLong1"},{"val":"myLong4","left":"myLong2","op":"Add","right":"myLong3"},{"val":"myLong5","left":"myLong4","op":"Add","right":"myLong2"},{"val":"myLong6","left":"myLong5","op":"Add","right":"myLong4"}],"unaryOps":[{"val":"myLong7","operand":"myLong2","op":"Sum"},{"val":"myLong8","operand":"myLong7","op":"Sum"}],"conversions":[{"to":"myErgoTree2","from":"myGroupElement","converter":"ProveDlog"},{"to":"myCollByte2","from":"myErgoTree2","converter":"ToCollByte"},{"to":"myAddress","from":"myErgoTree1","converter":"ToAddress"},{"to":"myIntToLong","from":"myInt","converter":"ToLong"}],"dataInputs":[{"address":{"value":"someAddress"},"registers":[{"name":"myRegister1","num":"R4","type":"CollByte","ref":"myTokenId"},{"name":"myRegister2","num":"R4","type":"CollByte","ref":"myCollByte"}],"tokens":[{"index":1,"numTokens":{"name":"someLong3","op":"Gt"}}],"nanoErgs":{"op":"Gt"}},{"address":{"ref":"myAddress"},"registers":[{"name":"myRegister4","num":"R4","type":"CollByte","ref":"myRegister2"}],"tokens":[{"index":1,"id":{"value":"123abc"},"numTokens":{"name":"someLong2","op":"Gt"}}],"nanoErgs":{"op":"Gt"}}],"inputs":[{"boxId":{"ref":"myCollByte"},"registers":[{"name":"myRegister3","num":"R4","type":"CollByte","ref":"myRegister1"}],"tokens":[{"index":1,"id":{"value":"123abc"},"numTokens":{"name":"someLong1","op":"Le"}}],"nanoErgs":{"op":"Gt"}}],"fee":{"ref":"myLong8"}}
        |""".stripMargin
    val strToProtocol = Parser.parse(str)
    offchain.compiler.Compiler.compile(protocol)
    require(protocolToJsonToProtocol == protocol, "protocol")
    require(strToProtocol == protocol)
  }
}
