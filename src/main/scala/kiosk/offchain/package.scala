package kiosk

package object offchain {
  case class Constant(name: String, `type`: DataType.Type, value: String)(implicit val dictionary: Dictionary) extends Defines {
    override lazy val isMulti: Option[Boolean] = Some(false)
    if (!DataType.validExternalTypes.contains(`type`))
      throw new Exception(s"Invalid type ${`type`} for $name. Permitted types are [${DataType.validExternalTypes.map(_.toString).reduceLeft(_ + ", " + _)}]")
    override lazy val defines = Some(Variable(name, `type`))
    override lazy val isLazy = false
  }

  case class BinaryOpResult(name: String, left: String, op: BinaryOp.Op, right: String)(implicit val dictionary: Dictionary) extends Refers with Defines {
    override lazy val isMulti: Option[Boolean] = None
    override lazy val references = Seq(Variable(left, DataType.Lazy), Variable(right, DataType.Lazy))
    override lazy val referrer = Some(name)
    override lazy val defines: Option[Variable] = Some(Variable(name, DataType.Lazy))
    override lazy val isLazy = true
  }

  case class UnaryOpResult(name: String, operand: String, op: UnaryOp.Op)(implicit val dictionary: Dictionary) extends Refers with Defines {
    override lazy val isMulti: Option[Boolean] = None
    override lazy val references = Seq(Variable(name, DataType.Lazy))
    override lazy val referrer = Some(name)
    override lazy val defines: Option[Variable] = Some(Variable(name, DataType.Lazy))
    override lazy val isLazy = true
  }

  case class UnaryConverterResult(name: String, operand: String, converter: UnaryConverter.Converter)(implicit val dictionary: Dictionary) extends Refers with Defines {
    override lazy val isMulti: Option[Boolean] = None
    lazy val unaryConverterTypes = UnaryConverter.getTypes(converter)
    override lazy val references = Seq(Variable(name, unaryConverterTypes.inputType))
    override lazy val referrer = Some(name)
    override lazy val defines: Option[Variable] = Some(Variable(name, unaryConverterTypes.returnType))
    override lazy val isLazy = true
  }

  def atMostOne(obj: Any, options: Option[_]*): Unit = {
    if (options.count(_.isDefined) > 1) throw new Exception(s"At most one of ${options.toSeq} must be defined in $obj")
  }

  def atLeastOne(obj: Any, options: Option[_]*): Unit = {
    if (options.count(_.isDefined) == 0) throw new Exception(s"At lease one of ${options.toSeq} must be defined in $obj")
  }
}
