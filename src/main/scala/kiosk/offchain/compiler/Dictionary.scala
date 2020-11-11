package kiosk.offchain.compiler

import kiosk.offchain.model.DataType

import scala.collection.mutable.{Map => MMap}

class Dictionary {

  private val dict = MMap[String, DictionaryObject]()
  private val lazyRefs = MMap[String, Seq[Variable]]()

  def print(isLazy: Boolean) = dict collect {
    case (key, value) if value.isUnresolved == isLazy =>
      println(s"$key: ${value.`type`}")
  }

  private def resolve(name: String, `type`: DataType.Type, stack: Seq[String]): Unit = {
    require(`type` != DataType.Unknown)
    dict.get(name) match {
      case Some(d) if Seq(`type`, DataType.Unknown).contains(d.`type`) =>
        if (d.isUnresolved) {
          dict -= name // remove temporarily
          lazyRefs(name).foreach(ref => resolve(ref.name, if (ref.`type` == DataType.Unknown) `type` else ref.`type`, stack :+ name))
          dict += name -> d.copy(isUnresolved = false, `type` = `type`)
        }
      case Some(d) =>
        throw new Exception(s"Invalid type found for $name. Need ${`type`}. Found ${d.`type`}. Stack $stack")
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

  def addDeclaration(declaration: Declaration) = {
    if (declaration.isLazy) {
      addLazyRefs(declaration.name.get, declaration.references)
    } else {
      declaration.references.map(reference => resolve(reference.name, reference.`type`, declaration.name.toSeq))
    }

    declaration.name.map(add(_, DictionaryObject(declaration.isLazy, declaration.`type`, declaration)))
  }

  addDeclaration(Height)
}
