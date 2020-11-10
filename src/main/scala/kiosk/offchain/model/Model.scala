package kiosk.offchain.model

import kiosk.offchain._
import kiosk.offchain.compiler.{Declaration, Variable}

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

case class RangeFilter(op: QuantifierOp.Op, value: Option[scala.Long], ref: Option[String]) extends Declaration {
  atMostOne(this, value, ref)
  override lazy val isLazy = false
  override lazy val defines = None
  override lazy val references = ref.map(refName => Variable(refName, DataType.Long)).toSeq
}

case class Address(name: Option[String], value: Option[String], ref: Option[String]) extends Declaration {
  atMostOne(this, value, ref)
  override lazy val isLazy = false
  override lazy val defines: Option[Variable] = name.map(defName => Variable(defName, DataType.Address))
  override lazy val references = ref.map(refName => Variable(refName, DataType.Address)).toSeq
}

case class Register(name: Option[String], num: RegNum.Num, `type`: DataType.Type, value: Option[String], ref: Option[String]) extends Declaration {
  atMostOne(this, value, ref)
  override lazy val isLazy = false
  override lazy val defines = name.map(defName => compiler.Variable(defName, `type`))
  override lazy val references = ref.map(refName => compiler.Variable(refName, `type`)).toSeq
}

case class CollByte(name: Option[String], value: Option[String], ref: Option[String]) extends Declaration {
  atMostOne(this, value, ref)
  override lazy val isLazy = false
  override lazy val defines = name.map(defName => Variable(defName, DataType.CollByte))
  override lazy val references = ref.map(refName => Variable(refName, DataType.CollByte)).toSeq
}

case class Long(name: Option[String], value: Option[scala.Long], ref: Option[String], filters: Option[Seq[RangeFilter]]) extends Declaration {
  atMostOne(this, value, ref, filters)
  override lazy val isLazy: Boolean = false
  override lazy val defines = name.map(defName => Variable(defName, DataType.Long))
  override lazy val references = ref.map(refName => Variable(refName, DataType.Long)).toSeq
}

case class Token(index: Option[Int], id: Option[CollByte], numTokens: Option[Long])

case class Input(boxId: Option[CollByte], address: Option[Address], registers: Option[Seq[Register]], tokens: Option[Seq[Token]], nanoErgs: Option[Long], boxCount: Option[Long]) {
  atLeastOne(this, boxId, address)
  def isMulti = boxCount.isDefined
}

case class Output(address: Address, registers: Option[Seq[Register]], tokens: Option[Seq[Token]], nanoErgs: Long)
