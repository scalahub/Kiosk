package kiosk.offchain

import scala.collection.mutable.{Map => MMap}

case class DictionaryObject(isLazy: Boolean, `type`: NamedType.Type, anyRef: AnyRef)

class Dictionary {
  private val lazyRefs = MMap[String, Seq[String]]()
  private val dict = MMap[String, DictionaryObject]()

  def reset = {
    dict.clear()
    lazyRefs.clear()
  }

  def resolve(name: String, allowedTypes: Seq[NamedType.Type]): Unit = {
    dict.get(name) match {
      case Some(dictionaryObject) if allowedTypes.contains(dictionaryObject.`type`) || allowedTypes.isEmpty =>
        if (dictionaryObject.isLazy) {
          dict -= name // remove temporarily
          lazyRefs(name).foreach(resolve(_, allowedTypes))
          dict += name -> dictionaryObject.copy(isLazy = false)
        }
      case _ => throw new Exception(s"Invalid or non-existent type $name found")
    }
  }

  def add(name: String, dictionaryObject: DictionaryObject) = {
    if (dict.contains(name)) throw new Exception(s"Variable $name already exists as ${dict(name)}")
    else dict += name -> dictionaryObject
  }

  def addLazyRefs(name: String, refs: Seq[String]) = {
    if (lazyRefs.contains(name)) throw new Exception(s"References for $name already exists as ${lazyRefs(name)}")
    else lazyRefs += name -> refs
  }
}

trait Refers {
  implicit val dictionary: Dictionary
  val variableName: Option[String] // referrer
  val references: Seq[String]
  val isLazy: Boolean
  val validRefTypes: Seq[NamedType.Type]

  if (references.toSet.size != references.size) throw new Exception(s"References for $variableName contain duplicates ${references.reduceLeft(_ + "," + _)}")
  if (isLazy) {
    dictionary.addLazyRefs(variableName.getOrElse(throw new Exception("Empty variableName not allowed for lazy references")), references)
  } else {
    references.map(dictionary.resolve(_, validRefTypes))
  }
}

trait Defines {
  implicit val dictionary: Dictionary
  val variableType: NamedType.Type
  val variableName: Option[String]
  val isLazy: Boolean

  variableName.map(dictionary.add(_, DictionaryObject(isLazy, variableType, this)))
}
