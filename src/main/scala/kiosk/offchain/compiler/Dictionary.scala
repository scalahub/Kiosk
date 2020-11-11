package kiosk.offchain.compiler

import kiosk.offchain.model.DataType

import scala.collection.mutable.{Map => MMap}

class Dictionary {

  private val dict = MMap[String, DictionaryObject]()
  private val lazyRefs = MMap[String, Seq[Variable]]()

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

  private def resolve(name: String, `type`: DataType.Type, optIsMulti: Option[Boolean], stack: Seq[String]): Unit = {
    require(`type` != DataType.Lazy)
    dict.get(name) match {
      case Some(d) if (d.`type` == `type` || d.`type` == DataType.Lazy) && matchMulti(optIsMulti, d.optionMulti) =>
        if (d.isLazy) {
          dict -= name // remove temporarily
          lazyRefs(name).foreach(ref => resolve(ref.name, if (ref.`type` == DataType.Lazy) `type` else ref.`type`, optIsMulti, stack :+ name))
          dict += name -> d.copy(isLazy = false, `type` = `type`)
        }
      case Some(d) =>
        throw new Exception(s"Invalid type found for $name. Need ${`type`} (multi: $optIsMulti). Found ${d.`type`} (multi: ${d.optionMulti}). Stack $stack")
      case _ =>
        throw new Exception(s"Reference to undefined variable $name: ${`type`}")
    }
  }

  private def add(name: String, dictionaryObject: DictionaryObject) = {
    if (dict.contains(name)) throw new Exception(s"Variable $name already exists as ${dict(name).`type`}")
    else dict += name -> dictionaryObject
  }

  private def addLazyRefs(name: String, refs: Seq[Variable]) = {
    if (lazyRefs.contains(name)) throw new Exception(s"References for $name already exist as ${lazyRefs(name)}")
    else lazyRefs += name -> refs
  }

  def addDeclaration(declaration: Declaration, optIsMulti: Option[Boolean]) = {
    if (declaration.isLazy) {
      addLazyRefs(
        declaration.name.get,
        declaration.references
      )
    } else {
      declaration.references.map(reference => resolve(reference.name, reference.`type`, optIsMulti, declaration.name.toSeq))
    }
    declaration.name.map(add(_, DictionaryObject(declaration.isLazy, declaration.`type`, declaration, optIsMulti)))
  }

  addDeclaration(Height, Some(false))
}
