package kiosk.offchain.compiler

import kiosk.ergo.KioskType
import kiosk.offchain.model.DataType

trait Declaration {
  var `type`: DataType.Type

  protected val maybeId: Option[String] // the name of the object declared. Option because not every Declaration needs a name, only those that must be referenced in another Declaration
  protected val refs: Seq[String] // names of the other Declarations referenced by this
  protected val refTypes: Seq[DataType.Type] // types of the other Declarations referenced by this

  val isLazy: Boolean
  val possiblyOnChain: Boolean

  lazy val id = maybeId.getOrElse(randId)

  lazy val isOnChainVariable = maybeId.nonEmpty && possiblyOnChain

  lazy val onChainVariable: Option[Variable] = if (isOnChainVariable) Some(Variable(randId, `type`)) else None
  lazy val references: Seq[Variable] = (refs zip refTypes).map { case (ref, refType) => Variable(ref, refType) } ++ onChainVariable

  def updateType(newType: DataType.Type) = `type` = newType
  def getValue(dictionary: Dictionary): KioskType[_] = if (isOnChainVariable) dictionary.getOnChainValue(onChainVariable.get.name) else dictionary.getValue(references.head.name)

  override def toString = id + ": " + `type`

  if (refs.toSet.size != refs.size) throw new Exception(s"Refs for $id contain duplicates ${refs.reduceLeft(_ + ", " + _)}")
}
