package kiosk

import kiosk.offchain.compiler.{Declaration, Variable}
import kiosk.offchain.model.{BinaryOperator, DataType, UnaryConverter, UnaryOperator}

package object offchain {
  case class Constant(`val`: String, `type`: DataType.Type, value: String) extends Declaration {
    require(`type` != DataType.Lazy, "Data type cannot be lazy")
    override lazy val name = Some(`val`)
    override lazy val refs = Nil
  }

  case class BinaryOp(`val`: String, left: String, op: BinaryOperator.Operator, right: String) extends Declaration {
    override lazy val name = Some(`val`)
    override lazy val refs = Seq(left, right)
    override lazy val `type` = DataType.Lazy
  }

  case class UnaryOp(`val`: String, operand: String, op: UnaryOperator.Operator) extends Declaration {
    override lazy val name = Some(`val`)
    override lazy val refs = Seq(operand)
    override lazy val `type` = DataType.Lazy
  }

  case class Conversion(`val`: String, operand: String, converter: UnaryConverter.Converter) extends Declaration {
    lazy val unaryConverterTypes = UnaryConverter.getTypes(converter)
    override lazy val name = Some(`val`)
    override lazy val refs = Seq(operand)
    override lazy val `type` = DataType.Lazy
  }

  def atMostOne(obj: Any, options: Option[_]*): Unit = {
    if (options.count(_.isDefined) > 1) throw new Exception(s"At most one of ${options.toSeq} must be defined in $obj")
  }

  def atLeastOne(obj: Any, options: Option[_]*): Unit = {
    if (options.count(_.isDefined) == 0) throw new Exception(s"At lease one of ${options.toSeq} must be defined in $obj")
  }
}
