package kiosk.offchain.compiler

import kiosk.offchain.model.DataType

import scala.collection.mutable.{Map => MMap}

case class DictionaryObject(isLazy: Boolean, `type`: DataType.Type, anyRef: AnyRef, optionMulti: Option[Boolean])

case class Variable(name: String, `type`: DataType.Type)

trait Declaration {
  val `type`: DataType.Type
  val name: Option[String]
  val refs: Seq[String]
  val isLazy = `type` == DataType.Lazy

  if (refs.toSet.size != refs.size) throw new Exception(s"References for $name contain duplicates ${refs.reduceLeft(_ + ", " + _)}")
  if (isLazy && name.isEmpty) throw new Exception("Empty 'val' not allowed for lazy references")
}

object InternalMethod

class Dictionary {
  val reservedTypes = Seq("HEIGHT" -> DictionaryObject(isLazy = false, `type` = DataType.Int, InternalMethod, Some(false)))

  private val dict = MMap[String, DictionaryObject]()
  private val lazyRefs = MMap[String, Seq[Variable]]()

  private def addReservedTypes = reservedTypes foreach { case (name, dictionaryObject) => add(name, dictionaryObject) }

  addReservedTypes

  def print(isLazy: Boolean) = dict collect {
    case (key, value) if value.isLazy == isLazy && value.optionMulti == Some(true) =>
      println(s"$key: Multi[${value.`type`}]")
    case (key, value) if value.isLazy == isLazy =>
      println(s"$key: ${value.`type`}")
  }

  private def matchMulti(left: Option[Boolean], right: Option[Boolean]) = {
    (left, right) match {
      case (a, b) if a == b    => true
      case (Some(true), _)     => true
      case (Some(false), None) => true
      case _                   => false
    }
  }

  private def resolve(name: String, t: DataType.Type, isMulti: Option[Boolean]): Unit = {
    require(t != DataType.Lazy)
    dict.get(name) match {
      case Some(d) if (d.`type` == t || d.`type` == DataType.Lazy) && matchMulti(isMulti, d.optionMulti) =>
        if (d.isLazy) {
          dict -= name // remove temporarily
          lazyRefs(name).foreach(ref => resolve(ref.name, if (ref.`type` == DataType.Lazy) t else ref.`type`, isMulti))
          dict += name -> d.copy(isLazy = false, `type` = t)
        }
      case Some(d) =>
        throw new Exception(s"Invalid type found for $name. Need $t (multi: $isMulti). Found ${d.`type`} (multi: ${d.optionMulti})")
      case _ =>
        throw new Exception(s"Reference to undefined variable $name: $t")
    }
  }

  private def add(name: String, dictionaryObject: DictionaryObject) = {
    if (dict.contains(name)) throw new Exception(s"Variable $name already exists as ${dict(name)}")
    else dict += name -> dictionaryObject
  }

  private def addLazyRefs(name: String, refs: Seq[Variable]) = {
    if (lazyRefs.contains(name)) throw new Exception(s"References for $name already exist as ${lazyRefs(name)}")
    else lazyRefs += name -> refs
  }

  def addDeclaration(declaration: Declaration, optionMulti: Option[Boolean]) = {
    if (declaration.isLazy) {
      addLazyRefs(declaration.name.get, declaration.refs.map(Variable(_, declaration.`type`)))
    } else {
      declaration.refs.map(ref => resolve(ref, declaration.`type`, optionMulti))
    }
    declaration.name.map(name => add(name, DictionaryObject(declaration.isLazy, declaration.`type`, this, optionMulti)))
  }
}
