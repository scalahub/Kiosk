package kiosk.offchain

import scala.collection.mutable.{Map => MMap}

case class DictionaryObject(isLazy: Boolean, t: DataType.Type, anyRef: AnyRef, isMulti: Option[Boolean])

object InternalMethod

class Dictionary {
  val reservedTypes = Seq("HEIGHT" -> DictionaryObject(isLazy = false, t = DataType.Int, InternalMethod, Some(false)))

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

  def matchMulti(left: Option[Boolean], right: Option[Boolean]) = {
    (left, right) match {
      case (a, b) if a == b    => true
      case (Some(true), _)     => true
      case (Some(false), None) => true
      case _                   => false
    }
  }

  def resolve(name: String, t: DataType.Type, isMulti: Option[Boolean]): Unit = {
    println(s"Resolving $name: ${t}")
    require(t != DataType.Lazy)
    dict.get(name) match {
      case Some(d) if (d.t == t || d.t == DataType.Lazy) && matchMulti(isMulti, d.isMulti) =>
        if (d.isLazy) {
          dict -= name // remove temporarily
          lazyRefs(name).foreach(ref => resolve(ref.name, if (ref.`type` == DataType.Lazy) t else ref.`type`, isMulti))
          dict += name -> d.copy(isLazy = false, t = t)
        }
      case Some(d) =>
        throw new Exception(s"Invalid type found for $name. Need $t (multi: $isMulti). Found ${d.t} (multi: ${d.isMulti})")
      case _ =>
        throw new Exception(s"Reference to undefined variable $name: $t")
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
  val isMulti: Option[Boolean]
  val referrer: Option[String]
  val references: Seq[Variable]
  val isLazy: Boolean

  if (references.toSet.size != references.size) throw new Exception(s"References for $referrer contain duplicates ${references.map(_.name).reduceLeft(_ + ", " + _)}")

  if (isLazy) {
    dictionary.addLazyRefs(referrer.getOrElse(throw new Exception("Empty variableName not allowed for lazy references")), references)
  } else {
    references.map(reference => dictionary.resolve(reference.name, reference.`type`, isMulti))
  }
}

trait Defines {
  implicit val dictionary: Dictionary
  val isMulti: Option[Boolean]
  val defines: Option[Variable]
  val isLazy: Boolean
  defines.map(define => dictionary.add(define.name, DictionaryObject(isLazy, define.`type`, this, isMulti)))
  println(s"Defined $defines")
}
