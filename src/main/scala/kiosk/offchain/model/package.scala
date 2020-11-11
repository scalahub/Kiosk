package kiosk.offchain

import kiosk.offchain.compiler._

package object model {

  /*
   Order of evaluation (resolution of variables):
     constants
     dataInputs
     inputs
     outputs,
     fee
     binaryOps/unaryOps/conversions (lazy)

     This means a variable defined in inputs can be referenced in dataInputs but not vice-versa
     Similarly a variable defined in first input can be referenced in the second input but not vice-versa

     lazy variables are not evaluated until needed
   */
  case class Protocol(constants: Option[Seq[Constant]],
                      binaryOps: Option[Seq[BinaryOp]],
                      unaryOps: Option[Seq[UnaryOp]],
                      conversions: Option[Seq[Conversion]],
                      dataInputs: Option[Seq[Input]],
                      inputs: Option[Seq[Input]],
                      outputs: Option[Seq[Output]],
                      fee: Option[Long])

  case class Address(name: Option[String], value: Option[String], ref: Option[String]) extends Declaration {
    atMostOne(this, value, ref)
    override lazy val refs = ref.toSeq
    override lazy val `type` = DataType.Address
    override lazy val refTypes = refs.map(_ => DataType.Address)
    override lazy val isLazy = false
  }

  case class Register(name: Option[String], num: RegNum.Num, `type`: DataType.Type, value: Option[String], ref: Option[String]) extends Declaration {
    atMostOne(this, value, ref)
    override lazy val refs = ref.toSeq
    override lazy val refTypes = refs.map(_ => `type`)
    override lazy val isLazy = false
  }

  case class CollByte(name: Option[String], value: Option[String], ref: Option[String]) extends Declaration {
    atMostOne(this, value, ref)
    override lazy val refs = ref.toSeq
    override lazy val `type` = DataType.CollByte
    override lazy val refTypes = refs.map(_ => DataType.CollByte)
    override lazy val isLazy = false
  }

  case class Long(name: Option[String], value: Option[scala.Long], ref: Option[String], op: Option[FilterOp.Op]) extends Declaration {
    atMostOne(this, value, ref)
    override lazy val refs = ref.toSeq
    override lazy val `type` = DataType.Long
    override lazy val refTypes = refs.map(_ => DataType.Long)
    override lazy val isLazy = false
  }

  case class Token(index: Option[Int], id: Option[CollByte], numTokens: Option[Long])

  case class Input(boxId: Option[CollByte], address: Option[Address], registers: Option[Seq[Register]], tokens: Option[Seq[Token]], nanoErgs: Option[Long]) {
    atLeastOne(this, boxId, address)
  }

  case class Output(address: Address, registers: Option[Seq[Register]], tokens: Option[Seq[Token]], nanoErgs: Long)
}
