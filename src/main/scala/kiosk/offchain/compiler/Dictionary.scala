package kiosk.offchain.compiler

import kiosk.appkit.Client
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

  def getInputNanoErgs = onChainInputs.map(_.nanoErgs.value).sum

  def getInputTokens =
    onChainInputs
      .flatMap(input => input.tokenIds.map(_.toString) zip input.tokenAmounts.map(_.value))
      .groupBy(_._1)
      .map { case (id, seq) => (id, seq.map(_._2).sum) }
      .toSeq

  def getInputBoxIds = onChainInputs.map(_.boxId.toString)

  def getDataInputBoxIds = onChainDataInputs.map(_.boxId.toString)

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

  private def add(declaration: Declaration): Unit = {
    val name = declaration.id
    val dictionaryObject = DictionaryObject(isUnresolved = declaration.isLazy, declaration)
    if (dict.contains(name)) throw new Exception(s"Variable $name already exists as ${dict(name).declaration}")
    else dict += name -> dictionaryObject
  }

  private def addLazyRefs(name: String, refs: Seq[Variable]) = {
    if (lazyRefs.contains(name)) throw new Exception(s"References for $name already exist as ${lazyRefs(name)}")
    else lazyRefs += name -> refs
  }

  private var commitBuffer: Seq[() => Unit] = Nil

  def commit = {
    commitBuffer.foreach(_.apply)
    commitBuffer = Nil
  }

  def addDeclarationLazily(declaration: Declaration) = {
    validateRefs(declaration)
    commitBuffer :+= (() => add(declaration))
  }

  def addDeclaration(declaration: Declaration) = {
    validateRefs(declaration)
    add(declaration)
  }

  private def validateRefs(declaration: Declaration) = {
    if (declaration.isLazy) {
      addLazyRefs(declaration.id, declaration.references)
    } else {
      declaration.references.map(reference => resolve(reference.name, reference.`type`, Seq(declaration.toString)))
    }
  }
  def addOnChainBoxMapping(name: String, f: (Seq[OnChainBox], Seq[OnChainBox]) => KioskType[_]) = onChainBoxMap += name -> f

  def addInput(onChainBox: OnChainBox) = onChainInputs :+= onChainBox

  def addDataInput(onChainBox: OnChainBox) = onChainDataInputs :+= onChainBox

  def getOnChainValue(name: String) = onChainBoxMap(name)(onChainDataInputs, onChainInputs)

  addDeclaration(height(Client.usingClient { ctx =>
    ctx.getHeight
  }))
}
