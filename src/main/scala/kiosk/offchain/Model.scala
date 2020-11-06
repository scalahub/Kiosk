package kiosk.offchain

abstract class LeftXorRight[T, S](left: Option[T], right: Option[S])(implicit dictionary: Dictionary) {
  if (!(left.isDefined ^ right.isDefined)) throw new Exception(s"Exactly one of $left or $right must be defined in $this")
}

abstract class ValueXorRef[T](value: Option[T], reference: Option[String])(implicit dictionary: Dictionary) extends LeftXorRight(value, reference) with Refers {
  override lazy val references: Seq[String] = reference.toSeq
}

case class Quantifier(op: QuantifierOp.Op, amount: Option[Long], ref: Option[String])(implicit val dictionary: Dictionary) extends ValueXorRef(amount, ref) {
  override lazy val variableName: Option[String] = None
  override lazy val validRefTypes = Seq(
    NamedType.Token, // copy the amount of token
    NamedType.InBox, // copy the nanoErgs of Box
    NamedType.OutBox, // copy the nanoErgs of Box
    NamedType.Variable // copy a variable (must be of Long or Int type)
  )
  override lazy val isLazy = false
}

case class Constant(name: String, `type`: DataType.Type, value: String)(implicit val dictionary: Dictionary) extends Defines {
  override lazy val variableName = Some(name)
  override lazy val variableType = NamedType.Variable
  override lazy val isLazy = false
}

case class BinaryOpResult(name: String, left: String, op: BinaryOp.Op, right: String)(implicit val dictionary: Dictionary) extends Refers with Defines {
  override lazy val references: Seq[String] = Seq(left, right)
  override lazy val validRefTypes = Nil
  override lazy val variableName = Some(name)
  override lazy val variableType = NamedType.Variable
  override lazy val isLazy = true
}

case class UnaryOpResult(name: String, input: String, op: UnaryOp.Op)(implicit val dictionary: Dictionary) extends Refers with Defines {
  override lazy val references = Seq(name)
  override lazy val validRefTypes = Nil
  override lazy val variableName = Some(name)
  override lazy val variableType = NamedType.Variable
  override lazy val isLazy = true
}

case class InputDefiner(name: Option[String],
                        id: Option[String], // boxId of the input or
                        address: Option[String], // address of the input
                        tokens: Option[Seq[TokenDefiner]],
                        registers: Option[Seq[RegisterDefiner]],
                        nanoErgs: Option[Quantifier])(implicit val dictionary: Dictionary)
    extends LeftXorRight(id, address)
    with Defines {
  override lazy val variableName = name
  override lazy val variableType = NamedType.InBox
  override lazy val isLazy = false
}

case class TokenDefiner(name: Option[String], index: Option[Int], id: Option[String], ref: Option[String], quantity: Option[Quantifier])(implicit val dictionary: Dictionary)
    extends ValueXorRef(id, ref)
    with Defines {
  override lazy val variableType = NamedType.Token
  override lazy val variableName = name
  override lazy val validRefTypes = Seq(
    NamedType.Reg, // copy the reg value (must be of type CollByte) to tokenId
    NamedType.InBox, // copy the boxId to tokenId
    NamedType.Variable, // copy the variable (must be of type CollByte) to tokenId
    NamedType.Token // copy the other tokenId to this tokenId
  )
  lazy val isLazy = false
}

case class RegisterDefiner(name: Option[String],
                           id: RegId.Id,
                           `type`: DataType.Type,
                           value: Option[String], // literal value or
                           ref: Option[String] // reference to other object in Dictionary to copy to this register
)(implicit val dictionary: Dictionary)
    extends ValueXorRef(value, ref)
    with Defines {
  override lazy val variableName = name
  override lazy val variableType = NamedType.Reg
  override lazy val validRefTypes = Seq(
    NamedType.Reg, // copy the other register to this register (both should be compatible types; example Int and Int)
    NamedType.InBox, // copy the boxId to register (must be of type CollByte)
    NamedType.Variable, // copy the variable to register (both should be compatible types; example Int and Int)
    NamedType.Token // copy the tokenId to register (must be of type CollByte)
  )
  lazy val isLazy = false
}

case class OutputDefiner(name: Option[String],
                         address: Option[String], // literal address or
                         ref: Option[String], // reference to other box whose address to copy
                         registers: Option[Seq[RegisterDefiner]],
                         tokens: Option[Seq[TokenDefiner]],
                         nanoErgs: Option[Quantifier])(implicit val dictionary: Dictionary)
    extends ValueXorRef(address, ref)
    with Defines {
  override lazy val variableName = name
  override lazy val variableType = NamedType.OutBox
  override lazy val validRefTypes = Seq(
    NamedType.InBox, // copy the address to this box
    NamedType.OutBox // copy the address to this box
  )
  override lazy val isLazy = false
}

/*
 Order of evaluation (resolution of variables):
   constants
   inputs
   dataInputs
   outputs
   binaryOps/unaryOps (lazy)

   This means a variable defined in inputs can be referenced in dataInputs but not vice-versa
   Similarly a variable defined in first input can be referenced in the second input but not vice-versa

   lazy variables are not evaluated until needed
 */
case class Protocol(constants: Option[Seq[Constant]],
                    binaryOps: Option[Seq[BinaryOpResult]],
                    unaryOps: Option[Seq[UnaryOpResult]],
                    inputs: Option[Seq[InputDefiner]],
                    dataInputs: Option[Seq[InputDefiner]],
                    outputs: Option[Seq[OutputDefiner]],
                    fee: Option[Long]) {
  val inputDefiners: Seq[InputDefiner] = inputs.toSeq.flatten
  val dataInputDefiners: Seq[InputDefiner] = dataInputs.toSeq.flatten
  val outputDefiners: Seq[OutputDefiner] = outputs.toSeq.flatten
  if (inputDefiners.size + outputDefiners.size + inputDefiners.size == 0) throw new Exception("At least one definer must be provided")
}
