package kiosk.offchain
import kiosk.offchain.DataType.Type

abstract class MyEnum extends Enumeration {
  def fromString(str: String): Value =
    values.find(value => value.toString.equalsIgnoreCase(str)).getOrElse(throw new Exception(s"Invalid op $str. Permitted options are ${values.map(_.toString).reduceLeft(_ + ", " + _)}"))
  def toString(op: Value): String = op.toString
}

object QuantifierOp extends MyEnum {
  type Op = Value
  val Le, Ge, Lt, Gt = Value
}

object DataType extends MyEnum {
  type Type = Value
  val Long, Int, CollByte, GroupElement, Address, ErgoTree, Lazy = Value

  val validExternalTypes = Seq(Long, Int, CollByte, GroupElement, Address, ErgoTree)
}

object RegNum extends MyEnum {
  type Num = Value
  val R4, R5, R6, R7, R8, R9 = Value
}

case class UnaryConverterTypes(inputType: Type, returnType: Type)

object BinaryOp extends MyEnum { // input and output types are same
  type Op = Value
  val Add, Sub, Mul, Div, Max, Min = Value
}

object UnaryOp extends MyEnum { // input and output types are same
  type Op = Value
  val Hash, Sum, Min, Max = Value
}

object UnaryConverter extends MyEnum { // input and output types are different
  type Converter = Value
  val ProveDlog, ToAddress, ToErgoTree, ToCollByte, ToLong, ToInt = Value
  def getTypes(converter: Converter) = {
    converter match {
      case ProveDlog  => UnaryConverterTypes(DataType.GroupElement, DataType.ErgoTree)
      case ToAddress  => UnaryConverterTypes(DataType.ErgoTree, DataType.Address)
      case ToErgoTree => UnaryConverterTypes(DataType.Address, DataType.ErgoTree)
      case ToCollByte => UnaryConverterTypes(DataType.ErgoTree, DataType.CollByte)
      case ToLong     => UnaryConverterTypes(DataType.Int, DataType.Long)
      case ToInt      => UnaryConverterTypes(DataType.Long, DataType.Int)
    }
  }
}
