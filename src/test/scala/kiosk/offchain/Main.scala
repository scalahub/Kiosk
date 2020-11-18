package kiosk.offchain

import kiosk.offchain
import kiosk.offchain.model._
import kiosk.offchain.parser.Parser

object Main {
  object Dummy {
    lazy val myLong1 = Constant("myLong1", DataType.Long, "1234")
    lazy val myInt = Constant("myInt", DataType.Int, "1234")
    lazy val myCollByte = Constant("myCollByte", DataType.CollByte, "77d14a018507949d1a88a631f76663e8e5101f57305dd5ebd319a41028d80456")
    lazy val myTokenId = Constant("myTokenId", DataType.CollByte, "77d14a018507949d1a88a631f76663e8e5101f57305dd5ebd319a41028d80456")
    lazy val myGroupElement = Constant("myGroupElement", DataType.GroupElement, "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67")
    lazy val myErgoTree1 = Constant("myErgoTree1", DataType.ErgoTree, "10010101D17300")
    lazy val myAddress = Constant("myAddress", DataType.Address, "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")

    lazy val myIntToLong = Conversion("myIntToLong", "myInt", UnaryConverter.ToLong)
    lazy val myLong2 = BinaryOp("myLong2", "myLong1", BinaryOperator.Add, "myIntToLong")
    lazy val myLong3 = BinaryOp("myLong3", "myLong2", BinaryOperator.Max, "myLong1")
    lazy val myLong4 = BinaryOp("myLong4", "myLong2", BinaryOperator.Add, "myLong3")
    lazy val myLong5 = BinaryOp("myLong5", "myLong4", BinaryOperator.Add, "myLong2")
    lazy val myLong6 = BinaryOp("myLong6", "myLong5", BinaryOperator.Add, "myLong4")

    lazy val myLong7 = UnaryOp("myLong7", "myLong2", UnaryOperator.Neg)
    lazy val myLong8 = UnaryOp("myLong8", "myLong7", UnaryOperator.Neg)

    lazy val myErgoTree2 = Conversion("myErgoTree2", "myGroupElement", UnaryConverter.ProveDlog)
    lazy val myCollByte2 = Conversion("myCollByte2", "myErgoTree2", UnaryConverter.ToCollByte)

    lazy val constants = Some(Seq(myLong1, myCollByte, myInt, myTokenId, myGroupElement, myErgoTree1, myAddress))
    lazy val unaryOps = Some(Seq(myLong7, myLong8))
    lazy val conversions = Some(Seq(myErgoTree2, myCollByte2, myIntToLong))
    lazy val binaryOps = Some(Seq(myLong2, myLong3, myLong4, myLong5, myLong6))

    lazy val myRegister1 = Register(Some("myRegister1"), value = None, RegNum.R4, DataType.CollByte)
    lazy val myRegister2 = Register(Some("myRegister2"), value = None, RegNum.R4, DataType.CollByte)
    lazy val myRegister3 = Register(Some("myRegister3"), value = None, RegNum.R4, DataType.CollByte)
    lazy val myRegister4 = Register(Some("myRegister4"), value = None, RegNum.R4, DataType.CollByte)

    lazy val myToken0 = Token(
      index = 1,
      tokenId = Id(name = Some("myToken1ActualId"), value = None),
      amount = model.Long(
        name = Some("someLong1"),
        value = None,
        filter = None
      )
    )

    lazy val myToken1 = Token(
      index = 1,
      tokenId = Id(name = Some("myToken1Id"), value = None),
      amount = model.Long(
        name = Some("someLong1"),
        value = None,
        filter = None
      )
    )

    lazy val myToken2 = Token(
      index = 1,
      tokenId = Id(name = Some("unreferencedToken2Id"), value = None),
      amount = model.Long(
        name = None,
        value = Some("myLong1"),
        filter = Some(FilterOp.Gt)
      )
    )

    lazy val myToken3 = Token(
      index = 1,
      tokenId = Id(name = Some("randomName"), value = None),
      amount = model.Long(
        name = Some("someLong3"),
        value = None,
        filter = None
      )
    )

    lazy val myInput1 = Input(
      boxId = Some(Id(name = None, value = Some("myCollByte"))),
      address = Some(Address(name = Some("myAddressName"), value = None)),
      registers = Some(Seq(myRegister3)),
      tokens = Some(Seq(myToken1)),
      nanoErgs = Some(model.Long(name = Some("input1NanoErgs"), value = None, filter = None))
    )

    lazy val myInput2 = Input(
      None,
      Some(Address(name = None, value = Some("myAddress"))),
      registers = Some(Seq(myRegister4)),
      tokens = Some(Seq(myToken2)),
      nanoErgs = Some(model.Long(name = Some("input2NanoErgs"), value = None, filter = None))
    )

    lazy val myInput3 = Input(
      boxId = None,
      address = Some(Address(name = None, value = Some("myAddress"))),
      registers = Some(Seq(myRegister1, myRegister2)),
      tokens = Some(Seq(myToken3)),
      nanoErgs = Some(model.Long(name = Some("input3NanoErgs"), value = None, filter = None))
    )

    lazy val protocol = Protocol(
      constants,
      dataInputs = Some(Seq(myInput3, myInput2)),
      inputs = Seq(myInput1),
      outputs = Nil,
      Some(Long(None, Some("myLong8"), Some(FilterOp.Eq))),
      binaryOps,
      unaryOps,
      conversions
    )
  }

  def main(args: Array[String]): Unit = {
    import Dummy._
    val protocolToJson = Parser.unparse(protocol)
    val protocolToJsonToProtocol = Parser.parse(protocolToJson.toString())
    println(protocolToJson)
    require(protocolToJsonToProtocol == protocol, "protocolToJsonToProtocol")
    val str =
      """{"constants":[{"name":"myLong1","type":"Long","value":"1234"},{"name":"myCollByte","type":"CollByte","value":"77d14a018507949d1a88a631f76663e8e5101f57305dd5ebd319a41028d80456"},{"name":"myInt","type":"Int","value":"1234"},{"name":"myTokenId","type":"CollByte","value":"77d14a018507949d1a88a631f76663e8e5101f57305dd5ebd319a41028d80456"},{"name":"myGroupElement","type":"GroupElement","value":"028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"},{"name":"myErgoTree1","type":"ErgoTree","value":"10010101D17300"},{"name":"myAddress","type":"Address","value":"9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"}],"dataInputs":[{"address":{"value":"myAddress"},"registers":[{"name":"myRegister1","num":"R4","type":"CollByte"},{"name":"myRegister2","num":"R4","type":"CollByte"}],"tokens":[{"index":1,"tokenId":{"name":"randomName"},"amount":{"name":"someLong3"}}],"nanoErgs":{"name":"input3NanoErgs"}},{"address":{"value":"myAddress"},"registers":[{"name":"myRegister4","num":"R4","type":"CollByte"}],"tokens":[{"index":1,"tokenId":{"name":"unreferencedToken2Id"},"amount":{"value":"myLong1","filter":"Gt"}}],"nanoErgs":{"name":"input2NanoErgs"}}],"inputs":[{"boxId":{"value":"myCollByte"},"registers":[{"name":"myRegister3","num":"R4","type":"CollByte"}],"tokens":[{"index":1,"tokenId":{"name":"myToken1Id"},"amount":{"name":"someLong1"}}],"nanoErgs":{"name":"input1NanoErgs"}}],"outputs":[],"fee":{"value":"myLong8","filter":"Eq"},"binaryOps":[{"name":"myLong2","first":"myLong1","op":"Add","second":"myIntToLong"},{"name":"myLong3","first":"myLong2","op":"Max","second":"myLong1"},{"name":"myLong4","first":"myLong2","op":"Add","second":"myLong3"},{"name":"myLong5","first":"myLong4","op":"Add","second":"myLong2"},{"name":"myLong6","first":"myLong5","op":"Add","second":"myLong4"}],"unaryOps":[{"out":"myLong7","in":"myLong2","op":"Neg"},{"out":"myLong8","in":"myLong7","op":"Neg"}],"conversions":[{"to":"myErgoTree2","from":"myGroupElement","converter":"ProveDlog"},{"to":"myCollByte2","from":"myErgoTree2","converter":"ToCollByte"},{"to":"myIntToLong","from":"myInt","converter":"ToLong"}]}
        |""".stripMargin
    val strToProtocol = Parser.parse(str)
//    require(strToProtocol == protocol, "strToProtocol")
    offchain.compiler.Compiler.compile(protocol)
  }
}
