package kiosk.offchain

import kiosk.ergo
import kiosk.offchain.compiler._

package object model {

  case class Protocol(constants: Option[Seq[Constant]],
                      dataInputs: Option[Seq[Input]],
                      inputs: Option[Seq[Input]],
                      outputs: Option[Seq[Output]],
                      fee: Option[Long],
                      binaryOps: Option[Seq[BinaryOp]],
                      unaryOps: Option[Seq[UnaryOp]],
                      conversions: Option[Seq[Conversion]])

  case class Address(name: Option[String], value: Option[String]) extends Declaration {
    override lazy val maybeId = name
    override lazy val refs = value.toSeq
    override var `type` = DataType.ErgoTree
    override lazy val refTypes = refs.map(_ => DataType.ErgoTree)
    override lazy val isLazy = false
  }

  case class Register(name: Option[String], num: RegNum.Num, var `type`: DataType.Type, value: Option[String]) extends Declaration {
    override lazy val maybeId = name
    override lazy val refs = value.toSeq
    override lazy val refTypes = refs.map(_ => `type`)
    override lazy val isLazy = false
  }

  case class CollByte(name: Option[String], value: Option[String]) extends Declaration {
    override lazy val maybeId = name
    override lazy val refs = value.toSeq
    override var `type` = DataType.CollByte
    override lazy val refTypes = refs.map(_ => DataType.CollByte)
    override lazy val isLazy = false
  }

  case class Long(name: Option[String], value: Option[String], filter: Option[FilterOp.Op]) extends Declaration {
    override lazy val maybeId = name
    override lazy val refs = value.toSeq
    override var `type` = DataType.Long
    override lazy val refTypes = refs.map(_ => DataType.Long)
    override lazy val isLazy = false
  }

  case class Token(index: Option[Int], tokenId: Option[CollByte], amount: Option[Long]) {
    // tokenId.map(id => id.maybeValue.map(hex => if (hex.size != 64) throw new Exception(s"Invalid tokenId $hex. Must be 64 chars")))
  }

  case class Input(boxId: Option[CollByte], address: Option[Address], registers: Option[Seq[Register]], tokens: Option[Seq[Token]], nanoErgs: Option[Long]) {
    atLeastOne(this, boxId, address)
    // boxId.map(id => id.maybeValue.map(hex => if (hex.size != 64) throw new Exception(s"Invalid boxId $hex. Must be 64 chars")))
  }

  case class Output(address: Address, registers: Option[Seq[Register]], tokens: Option[Seq[Token]], nanoErgs: Long)

  case class Constant(name: String, var `type`: DataType.Type, value: String) extends Declaration {
    require(`type` != DataType.Unknown, "Data type cannot be lazy")
    override lazy val maybeId = Some(name)
    override lazy val refs = Nil
    override lazy val refTypes = Nil
    override lazy val isLazy = true

    override def getValue(fromRefs: Seq[ergo.KioskType[_]]): ergo.KioskType[_] = DataType.getKioskValue(value, `type`)
  }

  case class Conversion(to: String, from: String, converter: UnaryConverter.Converter) extends Declaration {
    override lazy val maybeId = Some(to)
    override lazy val refs = Seq(from)

    lazy val types = UnaryConverter.getFromTo(converter)
    override var `type` = types.to

    override lazy val refTypes = Seq(types.from)
    override lazy val isLazy = true

    override def getValue(fromRefs: Seq[ergo.KioskType[_]]): ergo.KioskType[_] = UnaryConverter.convert(converter, fromRefs.head)
  }

  case class BinaryOp(name: String, first: String, op: BinaryOperator.Operator, second: String) extends Declaration {
    override lazy val maybeId = Some(name)
    override lazy val refs = Seq(first, second)
    override var `type` = DataType.Unknown
    override lazy val refTypes = Seq(DataType.Unknown, DataType.Unknown)
    override lazy val isLazy = true

    override def getValue(fromRefs: Seq[ergo.KioskType[_]]): ergo.KioskType[_] = BinaryOperator.operate(op, fromRefs(0), fromRefs(1))
  }

  case class UnaryOp(out: String, in: String, op: UnaryOperator.Operator) extends Declaration {
    override lazy val maybeId = Some(out)
    override lazy val refs = Seq(in)
    override var `type` = DataType.Unknown
    override lazy val refTypes = Seq(DataType.Unknown)
    override lazy val isLazy = true
  }

  def atLeastOne(obj: Any, options: Option[_]*): Unit = if (options.count(_.isDefined) == 0) throw new Exception(s"At lease one of ${options.toSeq} must be defined in $obj")

}
