package kiosk.offchain.model

abstract class MyEnum extends Enumeration {
  def fromString(str: String): Value =
    values.find(value => value.toString.equalsIgnoreCase(str)).getOrElse(throw new Exception(s"Invalid op $str. Permitted options are ${values.map(_.toString).reduceLeft(_ + ", " + _)}"))
  def toString(op: Value): String = op.toString
}

object FilterOp extends MyEnum {
  type Op = Value
  val Le, Ge, Lt, Gt = Value
}

object DataType extends MyEnum {
  type Type = Value
  val Long, Int, CollByte, GroupElement, Address, ErgoTree, Unknown = Value
}

object RegNum extends MyEnum {
  type Num = Value
  val R4, R5, R6, R7, R8, R9 = Value
}

object BinaryOperator extends MyEnum { // input and output types are same
  type Operator = Value
  val Add, Sub, Mul, Div, Max, Min = Value
}

object UnaryOperator extends MyEnum { // input and output types are same
  type Operator = Value
  val Hash, Sum, Min, Max = Value
}

case class FromTo(from: DataType.Type, to: DataType.Type)

object UnaryConverter extends MyEnum { // input and output types are different
  type Converter = Value
  val ProveDlog, ToAddress, ToErgoTree, ToCollByte, ToLong, ToInt = Value
  def getFromTo(converter: Converter) = {
    converter match {
      case ProveDlog  => FromTo(from = DataType.GroupElement, to = DataType.ErgoTree)
      case ToAddress  => FromTo(from = DataType.ErgoTree, to = DataType.Address)
      case ToErgoTree => FromTo(from = DataType.Address, to = DataType.ErgoTree)
      case ToCollByte => FromTo(from = DataType.ErgoTree, to = DataType.CollByte)
      case ToLong     => FromTo(from = DataType.Int, to = DataType.Long)
      case ToInt      => FromTo(from = DataType.Long, to = DataType.Int)
    }
  }
}
