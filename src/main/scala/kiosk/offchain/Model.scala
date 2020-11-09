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

trait Multi {
  implicit val isMulti: Boolean
}
abstract class AbstractAddress(val name: Option[String], val value: Option[String], val ref: Option[String])(implicit dictionary: Dictionary) extends Defines with Refers with Multi {
  atMostOne(this, value, ref)
  override lazy val isLazy = false
  override lazy val referrer = name
  override lazy val defines = name.map(defName => Variable(defName, DataType.getSeqType(DataType.Address)))
  override lazy val references = ref.map(refName => Variable(refName, DataType.getSeqType(DataType.Address))).toSeq
}

abstract class AbstractRegister(val name: Option[String], val num: RegNum.Num, val `type`: DataType.Type, val value: Option[String], val ref: Option[String])(implicit dictionary: Dictionary)
    extends Defines
    with Refers
    with Multi {
  atMostOne(this, value, ref)
  override lazy val referrer = name
  override lazy val defines = name.map(defName => Variable(defName, DataType.getSeqType(`type`)))
  override lazy val isLazy = false
  override lazy val references = ref.map(refName => Variable(refName, DataType.getSeqType(`type`))).toSeq
}

abstract class AbstractCollByte(val name: Option[String], val value: Option[String], val ref: Option[String])(implicit dictionary: Dictionary) extends Defines with Refers with Multi {
  atMostOne(this, value, ref)
  override lazy val isLazy = false
  override lazy val referrer = name
  override lazy val defines = name.map(defName => Variable(defName, DataType.getSeqType(DataType.CollByte)))
  override lazy val references = ref.map(refName => Variable(refName, DataType.getSeqType(DataType.CollByte))).toSeq
}

abstract class AbstractLong(val name: Option[String], val value: Option[scala.Long], val ref: Option[String], val filters: Option[Seq[RangeFilter]])(implicit dictionary: Dictionary)
    extends Defines
    with Multi {
  atMostOne(this, value, ref, filters)
  override lazy val defines = name.map(defName => Variable(defName, DataType.getSeqType(DataType.Long)))
  override lazy val isLazy: Boolean = false
}

case class RangeFilter(op: QuantifierOp.Op, value: Option[scala.Long], ref: Option[String])(implicit val dictionary: Dictionary) extends Refers {
  atMostOne(this, value, ref)
  override lazy val referrer: Option[String] = None
  override lazy val isLazy = false
  override lazy val references = ref.map(refName => Variable(refName, DataType.Long)).toSeq
}

case class Address(override val name: Option[String], override val value: Option[String], override val ref: Option[String])(implicit val dictionary: Dictionary)
    extends AbstractAddress(name, value, ref) {
  override implicit lazy val isMulti: Boolean = false
}

case class Register(override val name: Option[String], override val num: RegNum.Num, override val `type`: DataType.Type, override val value: Option[String], override val ref: Option[String])(
    implicit val dictionary: Dictionary)
    extends AbstractRegister(name, num, `type`, value, ref) {
  override implicit lazy val isMulti: Boolean = false
}

case class CollByte(override val name: Option[String], override val value: Option[String], override val ref: Option[String])(implicit val dictionary: Dictionary)
    extends AbstractCollByte(name, value, ref) {
  override implicit lazy val isMulti: Boolean = false
}

case class Long(override val name: Option[String], override val value: Option[scala.Long], override val ref: Option[String], override val filters: Option[Seq[RangeFilter]])(
    implicit val dictionary: Dictionary)
    extends AbstractLong(name, value, ref, filters) {
  override implicit lazy val isMulti: Boolean = false
}

case class Token(index: Option[Int], id: Option[CollByte], numTokens: Option[Long])(implicit val dictionary: Dictionary)

case class SingleInput(boxId: Option[CollByte], address: Option[Address], registers: Option[Seq[Register]], tokens: Option[Seq[Token]], nanoErgs: Option[Long])(implicit val dictionary: Dictionary) {
  atLeastOne(this, boxId, address)
}
case class SingleOutput(address: Address, registers: Option[Seq[Register]], tokens: Option[Seq[Token]], nanoErgs: Option[Long])(implicit val dictionary: Dictionary)

// following for matching multiple inputs/outputs
case class MultiLong(override val name: Option[String], override val value: Option[scala.Long], override val ref: Option[String], override val filters: Option[Seq[RangeFilter]])(
    implicit val dictionary: Dictionary)
    extends AbstractLong(name, value, ref, filters) {
  override implicit lazy val isMulti: Boolean = true
}

case class MultiRegister(override val name: Option[String], override val num: RegNum.Num, override val `type`: DataType.Type, override val value: Option[String], override val ref: Option[String])(
    implicit val dictionary: Dictionary)
    extends AbstractRegister(name, num, `type`, value, ref) {
  override implicit lazy val isMulti: Boolean = true
}

case class MultiCollByte(override val name: Option[String], override val value: Option[String], override val ref: Option[String])(implicit val dictionary: Dictionary)
    extends AbstractCollByte(name, value, ref) {
  override implicit lazy val isMulti: Boolean = true
}

case class MultiAddress(override val name: Option[String], override val value: Option[String], override val ref: Option[String])(implicit val dictionary: Dictionary)
    extends AbstractAddress(name, value, ref) {
  override implicit lazy val isMulti: Boolean = true
}

case class MultiToken(index: Option[Int], id: Option[MultiCollByte], numTokens: Option[MultiLong])(implicit val dictionary: Dictionary)

case class MultiInput(address: Option[MultiAddress], registers: Option[Seq[MultiRegister]], tokens: Option[Seq[MultiToken]], nanoErgs: Option[MultiLong], count: Option[Long])(
    implicit val dictionary: Dictionary)

case class MultiOutput(address: MultiAddress, registers: Option[Seq[MultiRegister]], tokens: Option[Seq[MultiToken]], nanoErgs: Option[MultiLong], count: Option[Long])(
    implicit val dictionary: Dictionary)
