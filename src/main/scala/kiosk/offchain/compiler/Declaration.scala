package kiosk.offchain.compiler

import kiosk.offchain.model.DataType

trait Declaration {
  val maybeId: Option[String] // the name of the object declared. Option because not every Declaration needs a name, only those that must be referenced in another Declaration
  val maybeValue: Option[String] // string encoded value of the object declared. Option because a Declaration can either have a string encoded value or refer to another Declaration of the same type
  var `type`: DataType.Type
  val refs: Seq[String] // names of the other Declarations referenced by this
  val refTypes: Seq[DataType.Type] // types of the other Declarations referenced by this

  def updateType(newType: DataType.Type) = {
    require(`type` == DataType.Unknown, s"Only Unknown type can be updated. Current type is ${`type`} for object $this (new type is $newType)") // only Unknown types should be updatable
    require(newType != DataType.Unknown, s"New type must not be Unknown for object $this") // new type should not be Unknown
    `type` = newType
    this
  }

  override def toString = maybeId.getOrElse("_") + ": " + `type`

  lazy val references = (refs zip refTypes) map { case (ref, refType) => Variable(ref, refType) }
  val isLazy: Boolean

  if (refs.size != refTypes.size) throw new Exception(s"Sizes of refs (${refs.size}) and refTypes (${refTypes.size}) do not match")
  if (refs.toSet.size != refs.size) throw new Exception(s"Refs for $maybeId contain duplicates ${refs.reduceLeft(_ + ", " + _)}")
  if (isLazy && maybeId.isEmpty) throw new Exception("Empty name not allowed for lazy references")
}
