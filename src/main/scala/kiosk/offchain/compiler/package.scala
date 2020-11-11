package kiosk.offchain

import kiosk.offchain.model.DataType.Type
import kiosk.offchain.model.{BinaryOperator, DataType, UnaryConverter, UnaryOperator}

package object compiler {
  case class DictionaryObject(isLazy: Boolean, `type`: DataType.Type, anyRef: Declaration, optionMulti: Option[Boolean])

  case class Variable(name: String, `type`: DataType.Type)

  object Height extends Declaration {
    override lazy val name: Option[String] = Some("HEIGHT")
    override lazy val refs: Seq[String] = Nil
    override lazy val refTypes: Seq[Type] = Nil
    override lazy val `type` = DataType.Int
  }

  trait Declaration {
    val name: Option[String]
    val `type`: DataType.Type
    val refs: Seq[String]
    val refTypes: Seq[DataType.Type]

    lazy val references = (refs zip refTypes) map { case (ref, refType) => Variable(ref, refType) }
    lazy val isLazy = `type` == DataType.Lazy

    if (refs.size != refTypes.size) throw new Exception(s"Sizes of refs (${refs.size}) and refTypes (${refTypes.size}) do not match")
    if (refs.toSet.size != refs.size) throw new Exception(s"Refs for $name contain duplicates ${refs.reduceLeft(_ + ", " + _)}")
    if (isLazy && name.isEmpty) throw new Exception("Empty name not allowed for lazy references")
  }

  case class Constant(`val`: String, `type`: DataType.Type, value: String) extends Declaration {
    require(`type` != DataType.Lazy, "Data type cannot be lazy")
    override lazy val name = Some(`val`)
    override lazy val refs = Nil
    override lazy val refTypes = Nil
  }

  case class Conversion(to: String, from: String, converter: UnaryConverter.Converter) extends Declaration {
    lazy val unaryConverterTypes = UnaryConverter.getFromTo(converter)
    override lazy val name = Some(to)
    override lazy val refs = Seq(from)
    lazy val types = UnaryConverter.getFromTo(converter)
    override lazy val `type` = types.to
    override lazy val refTypes = Seq(types.from)
  }

  case class BinaryOp(`val`: String, left: String, op: BinaryOperator.Operator, right: String) extends Declaration {
    override lazy val name = Some(`val`)
    override lazy val refs = Seq(left, right)
    override lazy val `type` = DataType.Lazy
    override lazy val refTypes = Seq(DataType.Lazy, DataType.Lazy)
  }

  case class UnaryOp(`val`: String, operand: String, op: UnaryOperator.Operator) extends Declaration {
    override lazy val name = Some(`val`)
    override lazy val refs = Seq(operand)
    override lazy val `type` = DataType.Lazy
    override lazy val refTypes = Seq(DataType.Lazy)
  }

  def atMostOne(obj: Any, options: Option[_]*): Unit = {
    if (options.count(_.isDefined) > 1) throw new Exception(s"At most one of ${options.toSeq} must be defined in $obj")
  }

  def atLeastOne(obj: Any, options: Option[_]*): Unit = {
    if (options.count(_.isDefined) == 0) throw new Exception(s"At lease one of ${options.toSeq} must be defined in $obj")
  }
}
