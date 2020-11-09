package kiosk.offchain

import scala.collection.mutable.{Map => MMap}

case class DictionaryObject(isLazy: Boolean, `type`: DataType.Type, anyRef: AnyRef)

object InternalMethod

class Dictionary {
  val reservedTypes = Seq("HEIGHT" -> DictionaryObject(isLazy = false, `type` = DataType.Int, InternalMethod))

  private val dict = MMap[String, DictionaryObject]()
  private val lazyRefs = MMap[String, Seq[Variable]]()

  def print = dict foreach {
    case (key, value) =>
      println(s"$key -> $value")
  }

  def addReservedTypes = reservedTypes foreach { case (name, dictionaryObject) => add(name, dictionaryObject) }

  addReservedTypes

  def reset = {
    dict.clear()
    lazyRefs.clear()
    addReservedTypes
  }
  def resolve(name: String, `type`: DataType.Type): Unit = {
    println(s"Resolving $name: ${`type`}")
    require(`type` != DataType.Lazy)
    dict.get(name) match {
      case Some(dictionaryObject) if dictionaryObject.`type` == `type` || dictionaryObject.`type` == DataType.Lazy =>
        if (dictionaryObject.isLazy) {
          dict -= name // remove temporarily
          lazyRefs(name).foreach(ref => resolve(ref.name, if (ref.`type` == DataType.Lazy) `type` else ref.`type`))
          dict += name -> dictionaryObject.copy(isLazy = false, `type` = `type`)
        }
      case Some(dictionaryObject) =>
        throw new Exception(s"Invalid type found for $name. Need ${`type`}. Found ${dictionaryObject.`type`}")
      case any =>
        throw new Exception(s"Reference to undefined variable $name")
    }
  }

  def add(name: String, dictionaryObject: DictionaryObject) = {
    if (dict.contains(name)) throw new Exception(s"Variable $name already exists as ${dict(name)}")
    else dict += name -> dictionaryObject
  }

  def addLazyRefs(name: String, refs: Seq[Variable]) = {
    if (lazyRefs.contains(name)) throw new Exception(s"References for $name already exist as ${lazyRefs(name)}")
    else lazyRefs += name -> refs
  }
}

case class Variable(name: String, `type`: DataType.Type)

trait Refers {
  implicit val dictionary: Dictionary
  val referrer: Option[String]
  val references: Seq[Variable]
  val isLazy: Boolean

  if (references.toSet.size != references.size) throw new Exception(s"References for $referrer contain duplicates ${references.map(_.name).reduceLeft(_ + ", " + _)}")

  if (isLazy) {
    dictionary.addLazyRefs(referrer.getOrElse(throw new Exception("Empty variableName not allowed for lazy references")), references)
  } else {
    references.map(reference => dictionary.resolve(reference.name, reference.`type`))
  }
}

trait Defines {
  implicit val dictionary: Dictionary
  val defines: Option[Variable]
  val isLazy: Boolean
  defines.map(define => dictionary.add(define.name, DictionaryObject(isLazy, define.`type`, this)))
  println(s"Defined $defines")
}
