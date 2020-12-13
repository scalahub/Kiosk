package kiosk.offchain.compiler

import kiosk.ergo
import kiosk.ergo.{KioskCollByte, KioskErgoTree, KioskInt, KioskLong}
import kiosk.offchain.compiler.model.DataType.Type
import kiosk.offchain.compiler.model.InputOptions.Options
import kiosk.offchain.compiler.model.RegNum.Num

import java.util.UUID

package object model {
  case class Protocol(constants: Option[Seq[Constant]],
                      // on-chain
                      auxInputs: Option[Seq[Input]], // for use in computation without having to add to data-inputs (or inputs)
                      dataInputs: Option[Seq[Input]],
                      inputs: Seq[Input],
                      // to-create
                      outputs: Seq[Output],
                      fee: Option[scala.Long],
                      // operations
                      binaryOps: Option[Seq[BinaryOp]],
                      unaryOps: Option[Seq[UnaryOp]],
                      conversions: Option[Seq[Conversion]],
                      branches: Option[Seq[Branch]]) {
    def withUuid(input: Input): (Input, UUID) = input -> UUID.randomUUID
    private[compiler] lazy val auxInputUuids: Option[Seq[(Input, UUID)]] = auxInputs.map(_.map(withUuid))
    private[compiler] lazy val dataInputUuids: Option[Seq[(Input, UUID)]] = dataInputs.map(_.map(withUuid))
    private[compiler] lazy val inputUuids: Seq[(Input, UUID)] = inputs.map(withUuid)
  }

  trait Box {}

  case class Input(id: Option[Id], address: Option[Address], registers: Option[Seq[Register]], tokens: Option[Seq[Token]], nanoErgs: Option[Long], options: Option[Set[InputOptions.Options]]) {
    atLeastOne(this)("id", "address")(id, address)
    private lazy val inputOptions: Set[Options] = options.getOrElse(Set.empty)
    lazy val strict: Boolean = inputOptions.contains(InputOptions.Strict) // applies to token matching only
    lazy val multi = inputOptions.contains(InputOptions.Multi) // ToDo
    lazy val optional = inputOptions.contains(InputOptions.Optional)
    for { boxId <- id; ergoTree <- address } exactlyOne(this)("id.name", "address.name")(boxId.name, ergoTree.name)
    if (optional) {
      requireEmpty(optSeq(registers).map(_.name -> "Optional input register.name"): _*)
      requireEmpty(
        id.flatMap(_.name) -> "Optional input id.name",
        address.flatMap(_.name) -> "Optional input address.name",
        nanoErgs.flatMap(_.name) -> "Optional input nanoErgs.name"
      )
      requireEmpty(optSeq(tokens).flatMap(token => Seq(token.id.flatMap(_.name) -> "Optional input token.id.name", token.amount.flatMap(_.name) -> "Optional input token.amount.name")): _*)
    }
  }

  case class Output(address: Address, registers: Option[Seq[Register]], tokens: Option[Seq[Token]], nanoErgs: Long) {
    optSeq(tokens).foreach(token => requireDefined(token.index -> "token index", token.id -> "token.id", token.amount -> "token amount"))
    optSeq(tokens).foreach(token =>
      for { id <- token.id; amount <- token.amount } requireEmpty(id.name -> "Output token.id.name", amount.name -> "Output token.amount.name", amount.filter -> "Output token.amount.filter"))
    requireEmpty(optSeq(registers).map(_.name -> "Output register.name"): _*)
    requireEmpty(address.name -> "Output address.name", nanoErgs.name -> "Output nanoErgs.name", nanoErgs.filter -> "Output nanoErgs.filter")
  }

  case class Address(name: Option[String], value: Option[String], values: Option[Seq[String]]) extends Declaration {
    override lazy val maybeTargetId = name
    override lazy val pointerNames: Seq[String] = value.toSeq ++ optSeq(values)
    override var `type` = DataType.Address
    override lazy val pointerTypes = pointerNames.map(_ => DataType.Address)
    override lazy val isLazy = false
    override lazy val canPointToOnChain: Boolean = true
    atLeastOne(this)("name", "value", "values")(name, value, values)
    for { _ <- value; _ <- values } exactlyOne(this)("value", "values")(value, values)
    for { _ <- name; _ <- value } exactlyOne(this)("name", "value")(name, value)
    values.map(valueSeq => require(valueSeq.size > 1, s"Values must contain at least two addresses in $this"))
    override def getValue(implicit dictionary: Dictionary): KioskErgoTree = super.getValue.asInstanceOf[KioskErgoTree]
    def getValues(implicit dictionary: Dictionary) = pointerNames.map(dictionary.getDeclaration(_).getValue.asInstanceOf[KioskErgoTree])
  }

  case class Register(name: Option[String], value: Option[String], num: Num, var `type`: Type) extends Declaration {
    override lazy val maybeTargetId = name
    override lazy val pointerNames = value.toSeq
    override lazy val pointerTypes = pointerNames.map(_ => `type`)
    override lazy val isLazy = false
    override lazy val canPointToOnChain: Boolean = true
    exactlyOne(this)("name", "value")(name, value)
  }

  case class Id(name: Option[String], value: Option[String]) extends Declaration {
    override lazy val maybeTargetId = name
    override lazy val pointerNames = value.toSeq
    override var `type` = DataType.CollByte
    override lazy val pointerTypes = pointerNames.map(_ => DataType.CollByte)
    override lazy val isLazy = false
    override lazy val canPointToOnChain: Boolean = true
    override def getValue(implicit dictionary: Dictionary): ergo.KioskCollByte = {
      val kioskCollByte = super.getValue.asInstanceOf[KioskCollByte]
      kioskCollByte.ensuring(kioskCollByte.arrayBytes.length == 32, s"Id $this (${kioskCollByte.hex}) size (${kioskCollByte.arrayBytes.length}) != 32")
    }
    exactlyOne(this)("name", "value")(name, value)
  }

  case class Long(name: Option[String], value: Option[String], filter: Option[FilterOp.Op]) extends Declaration {
    override lazy val maybeTargetId = name
    override lazy val pointerNames = value.toSeq
    override var `type` = DataType.Long
    override lazy val pointerTypes = pointerNames.map(_ => DataType.Long)
    override lazy val isLazy = false
    override lazy val canPointToOnChain: Boolean = true
    lazy val filterOp = filter.getOrElse(FilterOp.Eq)
    def getFilterTarget(implicit dictionary: Dictionary): Option[KioskLong] = value.map(dictionary.getDeclaration(_).getValue.asInstanceOf[KioskLong])
    override def getValue(implicit dictionary: Dictionary): ergo.KioskLong = super.getValue.asInstanceOf[KioskLong]
    if (filter.nonEmpty && value.isEmpty) throw new Exception(s"Value cannot be empty if filter is defined")
    if (filter.contains(FilterOp.Eq)) throw new Exception(s"Filter cannot be Eq")
    atLeastOne(this)("name", "value")(name, value)
    for { _ <- name; _ <- value } require(filter.isDefined, s"Filter must be defined if both name and values are defined")
  }

  case class Token(index: Option[Int], id: Option[Id], amount: Option[Long]) {
    index.map(int => require(int >= 0, s"Token index must be >= 0. $this"))
    atLeastOne(this)("index", "id")(index, id)
    id.foreach(someId => atLeastOne(someId)("index", "id.value")(index, someId.value))
  }

  case class Constant(name: String, var `type`: DataType.Type, value: String) extends Declaration {
    override lazy val maybeTargetId = Some(name)
    override lazy val pointerNames = Nil
    override lazy val pointerTypes = Nil
    override lazy val isLazy = true
    override def getValue(implicit dictionary: Dictionary): ergo.KioskType[_] = DataType.getValue(value, `type`)
    override lazy val canPointToOnChain: Boolean = false
    require(`type` != DataType.Unknown, "Data type cannot be unknown")
  }

  case class Conversion(to: String, from: String, converter: UnaryConverter.Converter) extends Declaration {
    override lazy val maybeTargetId = Some(to)
    override lazy val pointerNames = Seq(from)
    lazy val types = UnaryConverter.getFromTo(converter)
    override var `type` = types.to
    override lazy val pointerTypes = Seq(types.from)
    override lazy val isLazy = true
    override lazy val canPointToOnChain: Boolean = false
    override def getValue(implicit dictionary: Dictionary): ergo.KioskType[_] = UnaryConverter.convert(converter, dictionary.getDeclaration(from).getValue(dictionary))
  }

  case class BinaryOp(name: String, first: String, op: BinaryOperator.Operator, second: String) extends Declaration {
    override lazy val maybeTargetId = Some(name)
    override lazy val pointerNames = Seq(first, second)
    override var `type` = DataType.Unknown
    override lazy val pointerTypes = Seq(DataType.Unknown, DataType.Unknown)
    override lazy val isLazy = true
    override lazy val canPointToOnChain: Boolean = false
    override def getValue(implicit dictionary: Dictionary): ergo.KioskType[_] = BinaryOperator.operate(op, dictionary.getDeclaration(first).getValue, dictionary.getDeclaration(second).getValue)
  }

  case class UnaryOp(out: String, in: String, op: UnaryOperator.Operator) extends Declaration {
    override lazy val maybeTargetId = Some(out)
    override lazy val pointerNames = Seq(in)
    override var `type` = DataType.Unknown
    override lazy val pointerTypes = Seq(DataType.Unknown)
    override lazy val isLazy = true
    override lazy val canPointToOnChain: Boolean = false
    override def getValue(implicit dictionary: Dictionary): ergo.KioskType[_] = UnaryOperator.operate(op, dictionary.getDeclaration(in).getValue)
  }

  case class Condition(first: String, second: String, op: FilterOp.Op) {
    def evaluate(implicit dictionary: Dictionary) = {
      (dictionary.getDeclaration(first).getValue, dictionary.getDeclaration(second).getValue) match {
        case (left: KioskLong, right: KioskLong)                                   => FilterOp.matches(left.value, right.value, op)
        case (left: KioskLong, right: KioskInt)                                    => FilterOp.matches(left.value, right.value, op)
        case (left: KioskInt, right: KioskLong)                                    => FilterOp.matches(left.value, right.value, op)
        case (left: KioskInt, right: KioskInt)                                     => FilterOp.matches(left.value, right.value, op)
        case (left, right) if left.typeName == right.typeName && op == FilterOp.Eq => left.hex == right.hex
        case (left, right) if left.typeName == right.typeName && op == FilterOp.Ne => left.hex != right.hex
        case (left, right)                                                         => throw new Exception(s"Invalid types for $op: ${left.typeName},${right.typeName}")
      }
    }
  }

  case class Branch(name: String, ifTrue: String, ifFalse: String, condition: Condition) extends Declaration {
    override protected lazy val maybeTargetId: Option[String] = Some(name)
    override protected lazy val pointerNames: Seq[String] = Seq(ifTrue, ifFalse, condition.first, condition.second)
    override var `type` = DataType.Unknown
    override protected lazy val pointerTypes: Seq[Type] = Seq(DataType.Unknown, DataType.Unknown, DataType.Unknown, DataType.Unknown)
    override lazy val isLazy: Boolean = true
    override lazy val canPointToOnChain: Boolean = false
    override def getValue(implicit dictionary: Dictionary): ergo.KioskType[_] = (if (condition.evaluate) dictionary.getDeclaration(ifTrue) else dictionary.getDeclaration(ifFalse)).getValue
  }
}
