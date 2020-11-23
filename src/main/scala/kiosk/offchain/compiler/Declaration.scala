package kiosk.offchain.compiler

import kiosk.ergo.KioskType
import kiosk.offchain.compiler.model.DataType

trait Declaration {
  var `type`: DataType.Type

  protected val maybeId: Option[String] // the name of the object declared. Option because not every Declaration needs a name, only those that must be referenced in another Declaration
  protected val refs: Seq[String] // names of the other Declarations referenced by this
  protected val refTypes: Seq[DataType.Type] // types of the other Declarations referenced by this

  val isLazy: Boolean

  val canPointToOnChain: Boolean

  lazy val id = maybeId.getOrElse(randId)

  lazy val isOnChain = maybeId.isDefined && canPointToOnChain

  lazy val onChainVariable: Option[Variable] = if (isOnChain) Some(Variable(randId, `type`)) else None

  lazy val references: Seq[Variable] = (refs zip refTypes).map { case (ref, refType) => new Variable(ref, refType) } ++ onChainVariable

  def updateType(newType: DataType.Type) = `type` = newType

  def getValue(implicit dictionary: Dictionary): KioskType[_] = {
    if (isOnChain) {
      dictionary.getOnChainValue(onChainVariable.get.name)
    } else {
      dictionary.getRef(references.head.name).getValue
    }
  }

  override def toString = s"${maybeId.getOrElse("unnamed")}: ${`type`}"

  if (refs.toSet.size != refs.size) throw new Exception(s"Refs for $id contain duplicates ${refs.reduceLeft(_ + ", " + _)}")
}
