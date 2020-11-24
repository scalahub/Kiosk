package kiosk.offchain.compiler

import kiosk.ergo
import kiosk.ergo.{KioskCollByte, KioskLong}
import kiosk.offchain.compiler.model.DataType.Type
import kiosk.offchain.compiler.model.InputOptions.Options
import kiosk.offchain.compiler.model.RegNum.Num

package object model {
  case class Protocol(constants: Option[Seq[Constant]],
                      // on-chain
                      dataInputs: Option[Seq[Input]],
                      inputs: Seq[Input],
                      // to-create
                      outputs: Seq[Output],
                      fee: Option[scala.Long],
                      // operations
                      binaryOps: Option[Seq[BinaryOp]],
                      unaryOps: Option[Seq[UnaryOp]],
                      conversions: Option[Seq[Conversion]])

  case class Input(id: Option[Id], address: Option[Address], registers: Option[Seq[Register]], tokens: Option[Seq[Token]], nanoErgs: Option[Long], options: Option[Set[InputOptions.Options]]) {
    atLeastOne(this)("id", "address")(id, address)
    private lazy val inputOptions: Set[Options] = options.getOrElse(Set.empty)
    lazy val strict: Boolean = inputOptions.contains(InputOptions.Strict) // applies to token matching only
    lazy val multi = inputOptions.contains(InputOptions.Multi) // ToDo
    for { boxId <- id; ergoTree <- address } exactlyOne(this)("id.name", "address.name")(boxId.name, ergoTree.name)
  }

  case class Output(address: Address, registers: Option[Seq[Register]], tokens: Option[Seq[Token]], nanoErgs: Long) {
    require(address.name.isEmpty, s"Output declaration (address) cannot be named: ${address.name}")
    require(nanoErgs.name.isEmpty, s"Output declaration (nanoErgs) cannot be named: ${nanoErgs.name}")
    require(nanoErgs.filter.isEmpty, s"Output declaration (nanoErgs) cannot have a filter: ${nanoErgs.filter}")
    optSeq(registers).foreach(register => require(register.name.isEmpty, s"Output declaration (register) cannot be named: ${register.name}"))
    optSeq(tokens).foreach { token =>
      require(token.index.isDefined, s"Output declaration (token index) cannot be empty: ${token}")
      require(token.id.isDefined, s"Output declaration (token Id) cannot be empty: ${token.id}")
      require(token.amount.isDefined, s"Output declaration (token amount) cannot be empty: ${token.amount}")
      for { id <- token.id; amount <- token.amount } {
        require(id.name.isEmpty, s"Output declaration (token Id) cannot be named: ${id.name}")
        require(amount.name.isEmpty, s"Output declaration (token amount) cannot be named: ${amount.name}")
        require(amount.filter.isEmpty, s"Output declaration (token amount) cannot have a filter: ${amount.filter}")
      }
    }
  }

  case class Address(name: Option[String], value: Option[String]) extends Declaration {
    override lazy val maybeTargetId = name
    override lazy val pointerNames = value.toSeq
    override var `type` = DataType.Address
    override lazy val pointerTypes = pointerNames.map(_ => DataType.Address)
    override lazy val isLazy = false
    override lazy val canPointToOnChain: Boolean = true
    exactlyOne(this)("name", "value")(name, value)
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
      kioskCollByte.ensuring(kioskCollByte.arrayBytes.size == 32, s"Id $this (${kioskCollByte.hex}) size (${kioskCollByte.arrayBytes.size}) != 32")
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
    def getFilterTarget(implicit dictionary: Dictionary): Option[KioskLong] = value.map(dictionary.getRef(_).getValue.asInstanceOf[KioskLong])
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
    override def getValue(implicit dictionary: Dictionary): ergo.KioskType[_] = UnaryConverter.convert(converter, dictionary.getRef(from).getValue(dictionary))
  }

  case class BinaryOp(name: String, first: String, op: BinaryOperator.Operator, second: String) extends Declaration {
    override lazy val maybeTargetId = Some(name)
    override lazy val pointerNames = Seq(first, second)
    override var `type` = DataType.Unknown
    override lazy val pointerTypes = Seq(DataType.Unknown, DataType.Unknown)
    override lazy val isLazy = true
    override lazy val canPointToOnChain: Boolean = false
    override def getValue(implicit dictionary: Dictionary): ergo.KioskType[_] = BinaryOperator.operate(op, dictionary.getRef(first).getValue, dictionary.getRef(second).getValue)
  }

  case class UnaryOp(out: String, in: String, op: UnaryOperator.Operator) extends Declaration {
    override lazy val maybeTargetId = Some(out)
    override lazy val pointerNames = Seq(in)
    override var `type` = DataType.Unknown
    override lazy val pointerTypes = Seq(DataType.Unknown)
    override lazy val isLazy = true
    override lazy val canPointToOnChain: Boolean = false
    override def getValue(implicit dictionary: Dictionary): ergo.KioskType[_] = UnaryOperator.operate(op, dictionary.getRef(in).getValue)
  }
}
