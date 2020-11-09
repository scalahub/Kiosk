package kiosk.offchain

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
  val Long, Int, CollByte, GroupElement, Address, ErgoTree, Lazy, MultiLong, MultiCollByte, MultiInt, MultiGroupElement = Value

  val validExternalTypes = Seq(Long, Int, CollByte, GroupElement, Address, ErgoTree)

  def getSeqType(dataType: DataType.Type)(implicit isMulti: Boolean) =
    if (!isMulti) dataType
    else
      dataType match {
        case Long         => MultiLong
        case Int          => MultiInt
        case GroupElement => MultiGroupElement
        case CollByte     => MultiCollByte
        case any          => throw new Exception(s"No multi type defined for $dataType")
      }
}

object RegNum extends MyEnum {
  type Num = Value
  val R4, R5, R6, R7, R8, R9 = Value
}

case class UnaryConverterTypes(returnType: DataType.Type, inputType: DataType.Type)

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
      case ProveDlog  => UnaryConverterTypes(DataType.ErgoTree, DataType.GroupElement)
      case ToAddress  => UnaryConverterTypes(DataType.Address, DataType.ErgoTree)
      case ToErgoTree => UnaryConverterTypes(DataType.ErgoTree, DataType.Address)
      case ToCollByte => UnaryConverterTypes(DataType.CollByte, DataType.ErgoTree)
      case ToLong     => UnaryConverterTypes(DataType.Long, DataType.Int)
      case ToInt      => UnaryConverterTypes(DataType.Int, DataType.Long)
    }
  }
}
