package kiosk.offchain.compiler

import kiosk.ergo.KioskType
import kiosk.offchain.compiler.model.DataType

trait Declaration {
  var `type`: DataType.Type

  protected val maybeTargetId: Option[String] // the name of the object declared. Option because not every Declaration needs a name, only those that must be referenced in another Declaration
  protected val pointerNames: Seq[String] // names of the other Declarations referenced by this
  protected val pointerTypes: Seq[DataType.Type] // types of the other Declarations referenced by this

  val isLazy: Boolean

  val canPointToOnChain: Boolean

  lazy val targetId = maybeTargetId.getOrElse(randId)

  lazy val isOnChain = maybeTargetId.isDefined && canPointToOnChain

  lazy val onChainVariable: Option[Variable] = if (isOnChain) Some(Variable(randId, `type`)) else None

  lazy val pointers: Seq[Variable] = (pointerNames zip pointerTypes).map { case (pointerName, pointerType) => new Variable(pointerName, pointerType) } ++ onChainVariable

  def updateType(newType: DataType.Type) = `type` = newType

  def getValue(implicit dictionary: Dictionary): KioskType[_] = {
    if (isOnChain) {
      dictionary.getOnChainValue(onChainVariable.get.name)
    } else {
      dictionary.getRef(pointers.head.name).getValue
    }
  }

  override def toString = s"${maybeTargetId.getOrElse("unnamed")}: ${`type`}"

  if (pointerNames.toSet.size != pointerNames.size) throw new Exception(s"Refs for $targetId contain duplicates ${pointerNames.reduceLeft(_ + ", " + _)}")
}
