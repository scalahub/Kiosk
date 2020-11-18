package kiosk.offchain.compiler

import kiosk.ergo
import kiosk.ergo.KioskType
import kiosk.offchain.model.DataType

import scala.collection.mutable.{Map => MMap}

class Dictionary {

  private val dict = MMap[String, DictionaryObject]()
  private val lazyRefs = MMap[String, Seq[Variable]]()

  private var onChainInputs = Seq[OnChainBox]()
  private var onChainDataInputs = Seq[OnChainBox]()
  private val onChainBoxMap: MMap[String, (Seq[OnChainBox], Seq[OnChainBox]) => KioskType[_]] = MMap()

  def getValue(name: String): ergo.KioskType[_] = dict(name).declaration.getValue(this)

  def getDictionaryObjects(unresolved: Boolean): Seq[(String, DictionaryObject)] =
    dict
      .collect {
        case (key, dictionaryObject) if dictionaryObject.isUnresolved == unresolved => key -> dictionaryObject
      }
      .toSeq
      .sortBy(_._1)

  private def resolve(name: String, `type`: DataType.Type, stack: Seq[String]): Unit = {
    require(`type` != DataType.Unknown)
    dict.get(name) match {
      case Some(d) if Seq(`type`, DataType.Unknown) contains d.declaration.`type` =>
        if (d.isUnresolved) {
          dict -= name // remove temporarily
          lazyRefs(name).foreach(ref => resolve(ref.name, if (ref.`type` == DataType.Unknown) `type` else ref.`type`, stack :+ d.declaration.toString))
          if (d.declaration.`type` == DataType.Unknown) d.declaration.updateType(`type`)
          dict += name -> d.copy(isUnresolved = false)
        }
      case Some(d) =>
        throw new Exception(s"Needed $name: ${`type`}. Found ${d.declaration}. Stack $stack")
      case _ =>
        throw new Exception(s"Reference to undefined variable $name: ${`type`}. Stack $stack")
    }
  }

  private def add(name: String, dictionaryObject: DictionaryObject) = {
    if (dict.contains(name)) throw new Exception(s"Variable $name already exists as ${dict(name).declaration}")
    else dict += name -> dictionaryObject
  }

  private def addLazyRefs(name: String, refs: Seq[Variable]) = {
    if (lazyRefs.contains(name)) throw new Exception(s"References for $name already exist as ${lazyRefs(name)}")
    else lazyRefs += name -> refs
  }

  def addDeclaration(declaration: Declaration) = {
    if (declaration.isLazy) {
      addLazyRefs(declaration.id, declaration.references)
    } else {
      declaration.references.map(reference => resolve(reference.name, reference.`type`, Seq(declaration.toString)))
    }
    add(declaration.id, DictionaryObject(isUnresolved = declaration.isLazy, declaration))
  }

  def addOnChainInput(onChainInput: OnChainBox) = onChainInputs :+= onChainInput

  def addRealDataInput(onChainDataInput: OnChainBox) = onChainDataInputs :+= onChainDataInput

  def getOnChainValue(name: String) = onChainBoxMap(name)(onChainDataInputs, onChainInputs)

  def addRealBoxMapping(name: String, f: (Seq[OnChainBox], Seq[OnChainBox]) => KioskType[_]) = onChainBoxMap += name -> f

  addDeclaration(height)
}
