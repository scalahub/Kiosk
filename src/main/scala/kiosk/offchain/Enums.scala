package kiosk.offchain

abstract class MyEnum extends Enumeration {
  def fromString(str: String): Value =
    values.find(value => value.toString.equalsIgnoreCase(str)).getOrElse(throw new Exception(s"Invalid op $str. Permitted options are ${values.map(_.toString).reduceLeft(_ + "," + _)}"))
  def toString(op: Value): String = op.toString
}

object QuantifierOp extends MyEnum {
  type Op = Value
  val Exactly, Minimum, MinimumTotal, MaximumTotal = Value
}

object DataType extends MyEnum {
  type Type = Value
  val Long, Int, CollByte, BigInt = Value
}

object RegId extends MyEnum {
  type Id = Value
  val R4, R5, R6, R7, R8, R9 = Value
}

object NamedType extends MyEnum {
  type Type = Value
  val InBox, OutBox, Reg, Token, Variable = Value
}

object BinaryOp extends MyEnum {
  type Op = Value
  val Add, Sub, Mul, Div, Max, Min, Exp = Value
}

object UnaryOp extends MyEnum {
  type Op = Value
  val Hash, GExp = Value
}
