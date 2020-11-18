package kiosk.offchain

import kiosk.ergo
import kiosk.ergo.KioskCollByte
import kiosk.offchain.compiler._
import kiosk.offchain.model.DataType.Type
import kiosk.offchain.model.RegNum.Num

package object model {
  case class Protocol(constants: Option[Seq[Constant]],
                      dataInputs: Option[Seq[Input]],
                      inputs: Seq[Input],
                      outputs: Seq[Output],
                      fee: Option[Long],
                      binaryOps: Option[Seq[BinaryOp]],
                      unaryOps: Option[Seq[UnaryOp]],
                      conversions: Option[Seq[Conversion]])

  case class Input(boxId: Option[Id], address: Option[Address], registers: Option[Seq[Register]], tokens: Option[Seq[Token]], nanoErgs: Option[Long]) {
    atLeastOne(this)("boxId", "address")(boxId, address)
    for { id <- boxId; ergoTree <- address } exactlyOne(this)("boxId.name", "address.name")(id.name, ergoTree.name)
  }

  case class Output(address: Address, registers: Option[Seq[Register]], tokens: Option[Seq[Token]], nanoErgs: Long) {
    require(address.name.isEmpty, s"Output declaration (address) cannot be named: ${address.name}")
    require(nanoErgs.name.isEmpty, s"Output declaration (nanoErgs) cannot be named: ${nanoErgs.name}")
    registers.toSeq.flatten.foreach(register => require(register.name.isEmpty, s"Output declaration (register) cannot be named: ${register.name}"))
    tokens.toSeq.flatten.foreach { token =>
      require(token.tokenId.name.isEmpty, s"Output declaration (token Id) cannot be named: ${token.tokenId.name}")
      require(token.amount.name.isEmpty, s"Output declaration (token amount) cannot be named: ${token.tokenId.name}")
    }
  }

  case class Address(name: Option[String], value: Option[String]) extends Declaration {
    override lazy val maybeId = name
    override lazy val refs = value.toSeq
    override var `type` = DataType.Address
    override lazy val refTypes = refs.map(_ => DataType.Address)
    override lazy val isLazy = false
    exactlyOne(this)("name", "value")(name, value)
  }

  case class Register(name: Option[String], value: Option[String], num: Num, var `type`: Type) extends Declaration {
    override lazy val maybeId = name
    override lazy val refs = value.toSeq
    override lazy val refTypes = refs.map(_ => `type`)
    override lazy val isLazy = false
    exactlyOne(this)("name", "value")(name, value)
  }

  case class Id(name: Option[String], value: Option[String]) extends Declaration {
    override lazy val maybeId = name
    override lazy val refs = value.toSeq
    override var `type` = DataType.CollByte
    override lazy val refTypes = refs.map(_ => DataType.CollByte)
    override lazy val isLazy = false
    override def getValue(dictionary: Dictionary): ergo.KioskType[_] = {
      val collByte = super.getValue(dictionary).asInstanceOf[KioskCollByte]
      collByte.ensuring(collByte.value.size == 32, s"Id $this (${collByte.hex}) must be exactly 32 bytes")
    }
    exactlyOne(this)("name", "value")(name, value)
  }

  case class Long(name: Option[String], value: Option[String], filter: Option[FilterOp.Op]) extends Declaration {
    override lazy val maybeId = name
    override lazy val refs = value.toSeq
    override var `type` = DataType.Long
    override lazy val refTypes = refs.map(_ => DataType.Long)
    override lazy val isLazy = false
    exactlyOne(this)("name", "value")(name, value)
    exactlyOne(this)("name", "filter")(name, filter)
  }

  case class Token(index: Int, tokenId: Id, amount: Long)

  case class Constant(name: String, var `type`: DataType.Type, value: String) extends Declaration {
    override lazy val maybeId = Some(name)
    override lazy val refs = Nil
    override lazy val refTypes = Nil
    override lazy val isLazy = true
    override def getValue(dictionary: Dictionary): ergo.KioskType[_] = DataType.getValue(value, `type`)
    override lazy val isConstant: Boolean = true
    require(`type` != DataType.Unknown, "Data type cannot be unknown")
  }

  case class Conversion(to: String, from: String, converter: UnaryConverter.Converter) extends Declaration {
    override lazy val maybeId = Some(to)
    override lazy val refs = Seq(from)
    lazy val types = UnaryConverter.getFromTo(converter)
    override var `type` = types.to
    override lazy val refTypes = Seq(types.from)
    override lazy val isLazy = true
    override def getValue(dictionary: Dictionary): ergo.KioskType[_] = UnaryConverter.convert(converter, dictionary.getValue(from))
  }

  case class BinaryOp(name: String, first: String, op: BinaryOperator.Operator, second: String) extends Declaration {
    override lazy val maybeId = Some(name)
    override lazy val refs = Seq(first, second)
    override var `type` = DataType.Unknown
    override lazy val refTypes = Seq(DataType.Unknown, DataType.Unknown)
    override lazy val isLazy = true
    override def getValue(dictionary: Dictionary): ergo.KioskType[_] = BinaryOperator.operate(op, dictionary.getValue(first), dictionary.getValue(second))
  }

  case class UnaryOp(out: String, in: String, op: UnaryOperator.Operator) extends Declaration {
    override lazy val maybeId = Some(out)
    override lazy val refs = Seq(in)
    override var `type` = DataType.Unknown
    override lazy val refTypes = Seq(DataType.Unknown)
    override lazy val isLazy = true
    override def getValue(dictionary: Dictionary): ergo.KioskType[_] = UnaryOperator.operate(op, dictionary.getValue(in))
  }
}
