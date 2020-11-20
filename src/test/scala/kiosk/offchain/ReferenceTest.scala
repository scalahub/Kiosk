package kiosk.offchain

import kiosk.offchain
import kiosk.offchain.model._
import kiosk.offchain.parser.Parser

object ReferenceTest {
  val myLong1 = Constant("myLong1", DataType.Long, "1234")
  val myInt = Constant("myInt", DataType.Int, "1234")
  val myCollByte = Constant("myCollByte", DataType.CollByte, "18d4ffec7a74662125340f3ed14da7d8af8ffd0624f9e2c69188de46c795c6e7")
  val myTokenId = Constant("myTokenId", DataType.CollByte, "18d4ffec7a74662125340f3ed14da7d8af8ffd0624f9e2c69188de46c795c6e7")
  val myGroupElement = Constant("myGroupElement", DataType.GroupElement, "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67")
  val myErgoTree1 = Constant("myErgoTree1", DataType.ErgoTree, "10010101D17300")
  val myAddress = Constant("myAddress", DataType.Address, "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")

  val myIntToLong = Conversion("myIntToLong", "myInt", UnaryConverter.ToLong)
  val myLong2 = BinaryOp("myLong2", "myLong1", BinaryOperator.Add, "myIntToLong")
  val myLong3 = BinaryOp("myLong3", "myLong2", BinaryOperator.Max, "myLong1")
  val myLong4 = BinaryOp("myLong4", "myLong2", BinaryOperator.Add, "myLong3")
  val myLong5 = BinaryOp("myLong5", "myLong4", BinaryOperator.Add, "myLong2")
  val myLong6 = BinaryOp("myLong6", "myLong5", BinaryOperator.Add, "myLong4")

  val myLong7 = UnaryOp("myLong7", "myLong2", UnaryOperator.Neg)
  val myLong8 = UnaryOp("myLong8", "myLong7", UnaryOperator.Neg)

  val myErgoTree2 = Conversion("myErgoTree2", "myGroupElement", UnaryConverter.ProveDlog)
  val myCollByte2 = Conversion("myCollByte2", "myErgoTree2", UnaryConverter.ToCollByte)

  val constants = Some(Seq(myLong1, myCollByte, myInt, myTokenId, myGroupElement, myErgoTree1, myAddress))
  val unaryOps = Some(Seq(myLong7, myLong8))
  val conversions = Some(Seq(myErgoTree2, myCollByte2, myIntToLong))
  val binaryOps = Some(Seq(myLong2, myLong3, myLong4, myLong5, myLong6))

  val myRegister1 = Register(Some("myRegister1"), value = None, RegNum.R4, DataType.CollByte)
  val myRegister2 = Register(Some("myRegister2"), value = None, RegNum.R4, DataType.CollByte)
  val myRegister3 = Register(Some("myRegister3"), value = None, RegNum.R4, DataType.CollByte)
  val myRegister4 = Register(Some("myRegister4"), value = None, RegNum.R4, DataType.CollByte)

  val myToken0 = Token(
    index = 1,
    id = Id(name = Some("myToken1ActualId"), value = None),
    amount = model.Long(
      name = Some("someLong1"),
      value = None,
      filter = None
    )
  )

  val myToken1 = Token(
    index = 1,
    id = Id(name = Some("myToken1Id"), value = None),
    amount = model.Long(
      name = Some("someLong1"),
      value = None,
      filter = None
    )
  )

  val myToken2 = Token(
    index = 1,
    id = Id(name = Some("unreferencedToken2Id"), value = None),
    amount = model.Long(
      name = None,
      value = Some("myLong1"),
      filter = Some(FilterOp.Gt)
    )
  )

  val myToken3 = Token(
    index = 1,
    id = Id(name = Some("randomName"), value = None),
    amount = model.Long(
      name = Some("someLong3"),
      value = None,
      filter = None
    )
  )

  val myInput1 = Input(
    id = Some(Id(name = None, value = Some("myCollByte"))),
    address = Some(Address(name = Some("myAddressName"), value = None)),
    registers = Some(Seq(myRegister3)),
    tokens = Some(Seq(myToken1)),
    nanoErgs = Some(model.Long(name = Some("input1NanoErgs"), value = None, filter = None))
  )

  val myInput2 = Input(
    None,
    Some(Address(name = None, value = Some("myAddress"))),
    registers = Some(Seq(myRegister4)),
    tokens = Some(Seq(myToken2)),
    nanoErgs = Some(model.Long(name = None, value = Some("input1NanoErgs"), filter = Some(FilterOp.Eq)))
  )

  val myInput3 = Input(
    id = None,
    address = Some(Address(name = None, value = Some("myAddress"))),
    registers = Some(Seq(myRegister1, myRegister2)),
    tokens = Some(Seq(myToken3)),
    nanoErgs = Some(model.Long(name = None, value = Some("someLong1"), filter = Some(FilterOp.Eq)))
  )

  val protocol = Protocol(
    constants,
    dataInputs = Some(Seq(myInput1, myInput2)),
    inputs = Seq(myInput3),
    outputs = Nil,
    fee = Some(10000L),
    binaryOps,
    unaryOps,
    conversions
  )

  def main(args: Array[String]): Unit = {
    val protocolToJson = Parser.unparse(protocol)
    val protocolToJsonToProtocol = Parser.parse(protocolToJson.toString())
    println(protocolToJson)
    require(protocolToJsonToProtocol == protocol, "protocolToJsonToProtocol")
    val str =
      """{"constants":[{"name":"myLong1","type":"Long","value":"1234"},{"name":"myCollByte","type":"CollByte","value":"18d4ffec7a74662125340f3ed14da7d8af8ffd0624f9e2c69188de46c795c6e7"},{"name":"myInt","type":"Int","value":"1234"},{"name":"myTokenId","type":"CollByte","value":"18d4ffec7a74662125340f3ed14da7d8af8ffd0624f9e2c69188de46c795c6e7"},{"name":"myGroupElement","type":"GroupElement","value":"028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"},{"name":"myErgoTree1","type":"ErgoTree","value":"10010101D17300"},{"name":"myAddress","type":"Address","value":"9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"}],"dataInputs":[{"id":{"value":"myCollByte"},"address":{"name":"myAddressName"},"registers":[{"name":"myRegister3","num":"R4","type":"CollByte"}],"tokens":[{"index":1,"id":{"name":"myToken1Id"},"amount":{"name":"someLong1"}}],"nanoErgs":{"name":"input1NanoErgs"}},{"address":{"value":"myAddress"},"registers":[{"name":"myRegister4","num":"R4","type":"CollByte"}],"tokens":[{"index":1,"id":{"name":"unreferencedToken2Id"},"amount":{"value":"myLong1","filter":"Gt"}}],"nanoErgs":{"value":"input1NanoErgs","filter":"Eq"}}],"inputs":[{"address":{"value":"myAddress"},"registers":[{"name":"myRegister1","num":"R4","type":"CollByte"},{"name":"myRegister2","num":"R4","type":"CollByte"}],"tokens":[{"index":1,"id":{"name":"randomName"},"amount":{"name":"someLong3"}}],"nanoErgs":{"value":"someLong1","filter":"Eq"}}],"outputs":[],"fee":10000,"binaryOps":[{"name":"myLong2","first":"myLong1","op":"Add","second":"myIntToLong"},{"name":"myLong3","first":"myLong2","op":"Max","second":"myLong1"},{"name":"myLong4","first":"myLong2","op":"Add","second":"myLong3"},{"name":"myLong5","first":"myLong4","op":"Add","second":"myLong2"},{"name":"myLong6","first":"myLong5","op":"Add","second":"myLong4"}],"unaryOps":[{"out":"myLong7","in":"myLong2","op":"Neg"},{"out":"myLong8","in":"myLong7","op":"Neg"}],"conversions":[{"to":"myErgoTree2","from":"myGroupElement","converter":"ProveDlog"},{"to":"myCollByte2","from":"myErgoTree2","converter":"ToCollByte"},{"to":"myIntToLong","from":"myInt","converter":"ToLong"}]}
        |""".stripMargin
    val strToProtocol = Parser.parse(str)
    require(strToProtocol == protocol, "strToProtocol")
    offchain.compiler.Compiler.compile(protocol)
  }
}
