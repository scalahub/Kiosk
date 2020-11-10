package kiosk

import kiosk.offchain.compiler
import kiosk.offchain.compiler.{Declaration, Variable}
import kiosk.offchain.model.{BinaryOperator, DataType, UnaryConverter, UnaryOperator}

package object offchain {
  case class Constant(name: String, `type`: DataType.Type, value: String) extends Declaration {
    if (!DataType.validExternalTypes.contains(`type`))
      throw new Exception(s"Invalid type ${`type`} for $name. Permitted types are [${DataType.validExternalTypes.map(_.toString).reduceLeft(_ + ", " + _)}]")
    override lazy val isLazy = false
    override lazy val defines = Some(Variable(name, `type`))
    override lazy val references = Nil
  }

  case class BinaryOp(name: String, left: String, op: BinaryOperator.Operator, right: String) extends Declaration {
    override lazy val references = Seq(Variable(left, DataType.Lazy), Variable(right, DataType.Lazy))
    override lazy val defines: Option[Variable] = Some(Variable(name, DataType.Lazy))
    override lazy val isLazy = true
  }

  case class UnaryOp(name: String, operand: String, op: UnaryOperator.Operator) extends Declaration {
    override lazy val references = Seq(Variable(name, DataType.Lazy))
    override lazy val defines: Option[Variable] = Some(Variable(name, DataType.Lazy))
    override lazy val isLazy = true
  }

  case class Conversion(name: String, operand: String, converter: UnaryConverter.Converter) extends Declaration {
    lazy val unaryConverterTypes = UnaryConverter.getTypes(converter)
    override lazy val references = Seq(compiler.Variable(name, unaryConverterTypes.inputType))
    override lazy val defines: Option[Variable] = Some(compiler.Variable(name, unaryConverterTypes.returnType))
    override lazy val isLazy = true
  }

  def atMostOne(obj: Any, options: Option[_]*): Unit = {
    if (options.count(_.isDefined) > 1) throw new Exception(s"At most one of ${options.toSeq} must be defined in $obj")
  }

  def atLeastOne(obj: Any, options: Option[_]*): Unit = {
    if (options.count(_.isDefined) == 0) throw new Exception(s"At lease one of ${options.toSeq} must be defined in $obj")
  }
}
