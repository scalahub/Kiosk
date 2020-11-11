package kiosk.offchain

import kiosk.offchain.compiler._

package object model {

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
  }

  case class Register(name: Option[String], num: RegNum.Num, `type`: DataType.Type, value: Option[String], ref: Option[String]) extends Declaration {
    atMostOne(this, value, ref)
    override lazy val refs = ref.toSeq
    override lazy val refTypes = refs.map(_ => `type`)
  }

  case class CollByte(name: Option[String], value: Option[String], ref: Option[String]) extends Declaration {
    atMostOne(this, value, ref)
    override lazy val refs = ref.toSeq
    override lazy val `type` = DataType.CollByte
    override lazy val refTypes = refs.map(_ => DataType.CollByte)
  }

  case class Filter(op: QuantifierOp.Op, value: Option[scala.Long], ref: Option[String]) extends Declaration {
    atMostOne(this, value, ref)
    override lazy val name = None
    override lazy val `type` = DataType.Long
    override lazy val refs = ref.toSeq
    override lazy val refTypes = refs.map(_ => DataType.Long)
  }

  case class Long(name: Option[String], value: Option[scala.Long], ref: Option[String], filters: Option[Seq[Filter]]) extends Declaration {
    atMostOne(this, value, ref, filters)
    override lazy val refs = ref.toSeq
    override lazy val `type` = DataType.Long
    override lazy val refTypes = refs.map(_ => DataType.Long)
  }

  case class Token(index: Option[Int], id: Option[CollByte], numTokens: Option[Long])

  /*
   If boxCount is defined then we will assume that the Input refers to a "multi" input, that is, a definition that matches multiple input boxes.
   To specify a single input (most common scenario), ensure that this value is kept empty.

   A "multi" definition is treated different to a single one even if the final number of boxes matched turn out to be 0 or 1 (because the compiler does not know in advance how many boxes will actually be matched)

   For instance a multi-address x corresponds to a sequence of addresses, not a single address.
   A single-address y cannot be assigned the value x but the reverse is possible.

   x = y  (allowed, single address of y will be copied to all boxes in x)
   y = x  (not allowed, since we cannot convert multiple rows to single row)

   The same rules apply for other types such as CollByte, Long, etc

   if a multi register x has index i
   and y is any single register then the following rules apply

   x = y  (allowed, value of y will be copied to register at index i of all boxes in x)
   y = x  (not allowed)
   */
  case class Input(boxId: Option[CollByte], address: Option[Address], registers: Option[Seq[Register]], tokens: Option[Seq[Token]], nanoErgs: Option[Long], boxCount: Option[Long]) {
    atLeastOne(this, boxId, address)
  }

  case class Output(address: Address, registers: Option[Seq[Register]], tokens: Option[Seq[Token]], nanoErgs: Long, numBoxes: Option[Long])
}
