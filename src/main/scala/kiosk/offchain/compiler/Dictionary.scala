package kiosk.offchain.compiler

import kiosk.offchain.model.DataType

import scala.collection.mutable.{Map => MMap}

case class DictionaryObject(isLazy: Boolean, `type`: DataType.Type, anyRef: AnyRef, isMulti: Option[Boolean])

case class Variable(name: String, `type`: DataType.Type)

trait Declaration {
  val defines: Option[Variable]
  val references: Seq[Variable]
  val isLazy: Boolean

  if (references.toSet.size != references.size) throw new Exception(s"References for $defines contain duplicates ${references.map(_.name).reduceLeft(_ + ", " + _)}")

  if (isLazy && defines.isEmpty) throw new Exception("Empty 'defines' not allowed for lazy references")
}

object InternalMethod

class Dictionary {
  val reservedTypes = Seq("HEIGHT" -> DictionaryObject(isLazy = false, `type` = DataType.Int, InternalMethod, Some(false)))

  private val dict = MMap[String, DictionaryObject]()
  private val lazyRefs = MMap[String, Seq[Variable]]()

  private def addReservedTypes = reservedTypes foreach { case (name, dictionaryObject) => add(name, dictionaryObject) }

  addReservedTypes

  def print(isLazy: Boolean) = dict collect {
    case (key, value) if value.isLazy == isLazy && value.isMulti == Some(true) =>
      println(s"$key: Multi[${value.`type`}]")
    case (key, value) if value.isLazy == isLazy =>
      println(s"$key: ${value.`type`}")
  }

  def matchMulti(left: Option[Boolean], right: Option[Boolean]) = {
    (left, right) match {
      case (a, b) if a == b       => true
      case (Some(true) | None, _) => true
      case (Some(false), None)    => true
      case _                      => false
    }
  }

  def resolve(name: String, t: DataType.Type, isMulti: Option[Boolean]): Unit = {
    require(t != DataType.Lazy)
    dict.get(name) match {
      case Some(d) if (d.`type` == t || d.`type` == DataType.Lazy) && matchMulti(isMulti, d.isMulti) =>
        if (d.isLazy) {
          dict -= name // remove temporarily
          lazyRefs(name).foreach(ref => resolve(ref.name, if (ref.`type` == DataType.Lazy) t else ref.`type`, isMulti))
          dict += name -> d.copy(isLazy = false, `type` = t)
        }
      case Some(d) =>
        throw new Exception(s"Invalid type found for $name. Need $t (multi: $isMulti). Found ${d.`type`} (multi: ${d.isMulti})")
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

  def addDeclaration(declaration: Declaration, isMulti: Option[Boolean]) = {
    if (declaration.isLazy) {
      addLazyRefs(declaration.defines.get.name, declaration.references)
    } else {
      declaration.references.map(reference => resolve(reference.name, reference.`type`, isMulti))
    }
    declaration.defines.map(define => add(define.name, DictionaryObject(declaration.isLazy, define.`type`, this, isMulti)))
  }
}
