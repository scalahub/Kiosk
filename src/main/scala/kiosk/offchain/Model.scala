package kiosk.offchain

/*
 Order of evaluation (resolution of variables):
   constants
   dataInputs
   inputs
   outputs
   binaryOps/unaryOps (lazy)

   This means a variable defined in inputs can be referenced in dataInputs but not vice-versa
   Similarly a variable defined in first input can be referenced in the second input but not vice-versa

   lazy variables are not evaluated until needed
 */
case class Protocol(constants: Option[Seq[Constant]],
                    binaryOps: Option[Seq[BinaryOpResult]],
                    unaryOps: Option[Seq[UnaryOpResult]],
                    unaryConverters: Option[Seq[UnaryConverterResult]],
                    dataInputs: Option[Seq[Either[SingleInput, MultiInput]]],
                    inputs: Option[Seq[Either[SingleInput, MultiInput]]],
                    outputs: Option[Seq[MultiOutput]],
                    fee: Option[Long])

case class Address(name: Option[String], value: Option[String], ref: Option[String])(implicit val dictionary: Dictionary) extends Defines with Refers {
  atMostOne(this, value, ref)
  override lazy val isLazy = false
  override lazy val referrer = name
  override lazy val defines = name.map(defName => Variable(defName, DataType.Address))
  override lazy val references = ref.map(refName => Variable(refName, DataType.Address)).toSeq
}

abstract class AbstractRegister(val name: Option[String], val num: RegNum.Num, val `type`: DataType.Type, val value: Option[String], val ref: Option[String])(implicit dictionary: Dictionary)
    extends Defines
    with Refers {
  atMostOne(this, value, ref)
  implicit val isMulti: Boolean
  override lazy val referrer = name
  override lazy val defines = name.map(defName => Variable(defName, DataType.getSeqType(`type`)))
  override lazy val isLazy = false
  override lazy val references = ref.map(refName => Variable(refName, DataType.getSeqType(`type`))).toSeq
}

case class Register(override val name: Option[String], override val num: RegNum.Num, override val `type`: DataType.Type, override val value: Option[String], override val ref: Option[String])(
    implicit val dictionary: Dictionary)
    extends AbstractRegister(name, num, `type`, value, ref) {
  override implicit lazy val isMulti: Boolean = false
}

case class MultiRegister(override val name: Option[String], override val num: RegNum.Num, override val `type`: DataType.Type, override val value: Option[String], override val ref: Option[String])(
    implicit val dictionary: Dictionary)
    extends AbstractRegister(name, num, `type`, value, ref) {
  override implicit lazy val isMulti: Boolean = true
}

abstract class AbstractCollByte(val name: Option[String], val value: Option[String], val ref: Option[String])(implicit dictionary: Dictionary) extends Defines with Refers {
  atMostOne(this, value, ref)
  implicit val isMulti: Boolean
  override lazy val isLazy = false
  override lazy val referrer = name
  override lazy val defines = name.map(defName => Variable(defName, DataType.getSeqType(DataType.CollByte)))
  override lazy val references = ref.map(refName => Variable(refName, DataType.getSeqType(DataType.CollByte))).toSeq
}

case class CollByte(override val name: Option[String], override val value: Option[String], override val ref: Option[String])(implicit val dictionary: Dictionary)
    extends AbstractCollByte(name, value, ref) {
  override implicit lazy val isMulti: Boolean = false
}

case class MultiCollByte(override val name: Option[String], override val value: Option[String], override val ref: Option[String])(implicit val dictionary: Dictionary)
    extends AbstractCollByte(name, value, ref) {
  override implicit lazy val isMulti: Boolean = true
}

case class Filter(op: QuantifierOp.Op, value: Option[scala.Long], ref: Option[String])(implicit val dictionary: Dictionary) extends Refers {
  atMostOne(this, value, ref)
  override lazy val referrer: Option[String] = None
  override lazy val isLazy = false
  override lazy val references = ref.map(refName => Variable(refName, DataType.Long)).toSeq
}

abstract class AbstractLong(val name: Option[String], val value: Option[scala.Long], val ref: Option[String], val filters: Option[Seq[Filter]])(implicit dictionary: Dictionary) extends Defines {
  atMostOne(this, value, ref, filters)
  implicit val isMulti: Boolean
  override lazy val defines = name.map(defName => Variable(defName, DataType.getSeqType(DataType.Long)))
  override lazy val isLazy: Boolean = false
}

case class Long(override val name: Option[String], override val value: Option[scala.Long], override val ref: Option[String], override val filters: Option[Seq[Filter]])(
    implicit val dictionary: Dictionary)
    extends AbstractLong(name, value, ref, filters) {
  override implicit lazy val isMulti: Boolean = false
}

case class MultiLong(override val name: Option[String], override val value: Option[scala.Long], override val ref: Option[String], override val filters: Option[Seq[Filter]])(
    implicit val dictionary: Dictionary)
    extends AbstractLong(name, value, ref, filters) {
  override implicit lazy val isMulti: Boolean = true
}

case class Token(index: Option[Int], id: Option[CollByte], numTokens: Option[Long])(implicit val dictionary: Dictionary)

case class MultiToken(index: Option[Int], id: Option[MultiCollByte], numTokens: Option[Long])(implicit val dictionary: Dictionary)

case class SingleInput(boxId: Option[CollByte], address: Option[Address], registers: Option[Seq[Register]], tokens: Option[Seq[Token]], nanoErgs: Option[Long])(implicit val dictionary: Dictionary) {
  atLeastOne(this, boxId, address)
}

case class SingleOutput(address: Address, registers: Option[Seq[Register]], tokens: Option[Seq[Token]], nanoErgs: Option[Long])(implicit val dictionary: Dictionary)

case class MultiInput(address: Address, registers: Option[Seq[MultiRegister]], tokens: Option[Seq[MultiToken]], nanoErgs: Option[MultiLong], count: Option[Long])(implicit val dictionary: Dictionary)

case class MultiOutput(address: Address, registers: Option[Seq[MultiRegister]], tokens: Option[Seq[MultiToken]], nanoErgs: Option[MultiLong], count: Long)(implicit val dictionary: Dictionary)
