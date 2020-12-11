package kiosk.offchain.compiler.model

import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo._
import kiosk.script.{KioskScriptCreator, KioskScriptEnv}
import scorex.crypto.hash.Blake2b256

abstract class MyEnum extends Enumeration {
  def fromString(str: String): Value =
    values.find(value => value.toString.equalsIgnoreCase(str)).getOrElse(throw new Exception(s"Invalid op $str. Permitted options are ${values.map(_.toString).reduceLeft(_ + ", " + _)}"))
  def toString(op: Value): String = op.toString
}

object FilterOp extends MyEnum {
  type Op = Value
  val Eq, Le, Ge, Lt, Gt, Ne = Value
  def matches(actual: scala.Long, required: scala.Long, op: FilterOp.Op) = {
    (actual, required, op) match {
      case (actual, required, Eq) if actual == required => true
      case (actual, required, Le) if actual <= required => true
      case (actual, required, Ge) if actual >= required => true
      case (actual, required, Lt) if actual < required  => true
      case (actual, required, Gt) if actual > required  => true
      case (actual, required, Ne) if actual != required => true
      case _                                            => false
    }
  }
}

object DataType extends MyEnum {
  type Type = Value
  val Long, Int, CollByte, GroupElement, ErgoTree, Address, Unknown = Value

  def getValue(stringValue: String, `type`: DataType.Type): KioskType[_] = {
    `type` match {
      case Long         => KioskLong(stringValue.toLong)
      case Int          => KioskInt(stringValue.toInt)
      case GroupElement => KioskGroupElement(ScalaErgoConverters.stringToGroupElement(stringValue))
      case CollByte     => KioskCollByte(stringValue.decodeHex)
      case ErgoTree     => KioskErgoTree(ScalaErgoConverters.stringToErgoTree(stringValue))
      case Address      => KioskErgoTree(ScalaErgoConverters.getAddressFromString(stringValue).script)
      case any          => throw new Exception(s"Unknown type $any")
    }
  }

  def isValid(value: KioskType[_], `type`: DataType.Type) = {
    (`type`, value) match {
      case (Long, _: KioskLong)                 => true
      case (Int, _: KioskInt)                   => true
      case (CollByte, _: KioskCollByte)         => true
      case (GroupElement, _: KioskGroupElement) => true
      case (ErgoTree, _: KioskErgoTree)         => true
      case (Address, _: KioskErgoTree)          => true
      case _                                    => false
    }
  }
}

object RegNum extends MyEnum {
  type Num = Value
  val R4, R5, R6, R7, R8, R9 = Value
  val valueMap = values.zipWithIndex.toMap
  def getIndex(reg: RegNum.Value): Int = valueMap(reg)
}

object BinaryOperator extends MyEnum { // input and output types are same
  type Operator = Value
  val Add, Sub, Mul, Div, Max, Min = Value

  def operate(operator: Operator, first: KioskType[_], second: KioskType[_]): KioskType[_] = {
    (operator, first, second) match {
      case (Add, KioskLong(a), KioskLong(b))                 => KioskLong(a + b)
      case (Sub, KioskLong(a), KioskLong(b))                 => KioskLong(a - b)
      case (Mul, KioskLong(a), KioskLong(b))                 => KioskLong(a * b)
      case (Div, KioskLong(a), KioskLong(b))                 => KioskLong(a / b)
      case (Max, KioskLong(a), KioskLong(b))                 => KioskLong(a max b)
      case (Min, KioskLong(a), KioskLong(b))                 => KioskLong(a min b)
      case (Add, KioskInt(a), KioskInt(b))                   => KioskInt(a + b)
      case (Sub, KioskInt(a), KioskInt(b))                   => KioskInt(a - b)
      case (Mul, KioskInt(a), KioskInt(b))                   => KioskInt(a * b)
      case (Div, KioskInt(a), KioskInt(b))                   => KioskInt(a / b)
      case (Max, KioskInt(a), KioskInt(b))                   => KioskInt(a max b)
      case (Min, KioskInt(a), KioskInt(b))                   => KioskInt(a min b)
      case (Add, KioskGroupElement(g), KioskGroupElement(h)) => KioskGroupElement(g.multiply(h))
      case (Sub, KioskGroupElement(g), KioskGroupElement(h)) => KioskGroupElement(g.multiply(h.negate))
      case (op, someFirst, someSecond)                       => throw new Exception(s"Invalid operation $op for ${someFirst.typeName}, ${someSecond.typeName}")
    }
  }
}

object UnaryOperator extends MyEnum { // input and output types are same
  type Operator = Value
  val Hash, Neg, Abs = Value
  def operate(operator: Operator, in: KioskType[_]): KioskType[_] = {
    (operator, in) match {
      case (Hash, KioskCollByte(a))    => KioskCollByte(Blake2b256(a))
      case (Neg, KioskGroupElement(g)) => KioskGroupElement(g.negate)
      case (Abs, KioskLong(a))         => KioskLong(a.abs)
      case (Neg, KioskLong(a))         => KioskLong(-a)
      case (Abs, KioskInt(a))          => KioskInt(a.abs)
      case (Neg, KioskInt(a))          => KioskInt(-a)
      case (op, someIn)                => throw new Exception(s"Invalid operation $op for ${someIn.typeName}")
    }
  }
}

case class FromTo(from: DataType.Type, to: DataType.Type)

object UnaryConverter extends MyEnum { // input and output types are different
  type Converter = Value
  val ProveDlog, ToCollByte, ToLong, ToInt, ToAddress, ToErgoTree = Value
  def getFromTo(converter: Converter) = {
    converter match {
      case ProveDlog  => FromTo(from = DataType.GroupElement, to = DataType.ErgoTree)
      case ToCollByte => FromTo(from = DataType.ErgoTree, to = DataType.CollByte)
      case ToLong     => FromTo(from = DataType.Int, to = DataType.Long)
      case ToInt      => FromTo(from = DataType.Long, to = DataType.Int)
      case ToAddress  => FromTo(from = DataType.ErgoTree, to = DataType.Address)
      case ToErgoTree => FromTo(from = DataType.Address, to = DataType.ErgoTree)
    }
  }

  def convert(converter: Converter, fromValue: KioskType[_]) = {
    converter match {
      case ProveDlog =>
        val g = fromValue.asInstanceOf[KioskGroupElement]
        val env = new KioskScriptEnv()
        env.$addIfNotExist("g", g)
        val compiler = new KioskScriptCreator(env)
        KioskErgoTree(compiler.$compile("proveDlog(g)"))
      case ToCollByte => KioskCollByte(fromValue.asInstanceOf[KioskErgoTree].serialize)
      case ToLong     => KioskLong(fromValue.asInstanceOf[KioskInt].value.toLong)
      case ToInt      => KioskInt(fromValue.asInstanceOf[KioskLong].value.toInt)
      case ToAddress  => fromValue.ensuring(_.isInstanceOf[KioskErgoTree])
      case ToErgoTree => fromValue.ensuring(_.isInstanceOf[KioskErgoTree])
    }
  }
}

object InputOptions extends MyEnum {
  type Options = Value
  val Strict, Multi, Optional = Value
}

object InputType extends MyEnum {
  type Type = Value
  val Aux, Data, Code /* code-input is the one being spent */ = Value
}
