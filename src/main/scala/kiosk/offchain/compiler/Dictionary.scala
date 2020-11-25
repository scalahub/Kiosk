package kiosk.offchain.compiler

import kiosk.appkit.Client
import kiosk.ergo.KioskType
import kiosk.offchain.compiler.model.DataType

import scala.collection.mutable.{Map => MMap}

class Dictionary(currentHeight: Int) {
  private val dict = MMap[String, DictionaryObject]()
  private val lazyRefs = MMap[String, Seq[Variable]]()

  private var onChainInputs = Seq[OnChainBox]()
  private var onChainDataInputs = Seq[OnChainBox]()
  private val onChainBoxMap: MMap[String, (Seq[OnChainBox], Seq[OnChainBox]) => KioskType[_]] = MMap()

  def getInputNanoErgs = onChainInputs.map(_.nanoErgs.value).sum

  def getInputTokens =
    onChainInputs
      .flatMap(input => input.stringTokenIds zip input.tokenAmounts.map(_.value))
      .groupBy(_._1)
      .map { case (id, seq) => (id, seq.map(_._2).sum) }
      .toSeq

  def getInputBoxIds = onChainInputs.map(_.boxId.toString)

  def getDataInputBoxIds = onChainDataInputs.map(_.boxId.toString)

  private[compiler] def getRef(name: String) = dict(name).declaration

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
    val name = declaration.targetId
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
      addLazyRefs(declaration.targetId, declaration.pointers)
    } else {
      declaration.pointers.map(reference => resolve(reference.name, reference.`type`, Seq(declaration.toString)))
    }
  }

  def addOnChainDeclaration(variable: Variable, isDataInput: Boolean, mapping: Seq[OnChainBox] => KioskType[_]) = {
    addDeclaration(OnChain(variable.name, variable.`type`))
    addOnChainBoxMapping(variable.name, (dataInput, input) => mapping(if (isDataInput) dataInput else input))
  }

  private def addOnChainBoxMapping(name: String, f: (Seq[OnChainBox], Seq[OnChainBox]) => KioskType[_]) = onChainBoxMap += name -> f

  def addInput(onChainBox: OnChainBox) = onChainInputs :+= onChainBox

  def addDataInput(onChainBox: OnChainBox) = onChainDataInputs :+= onChainBox

  def getOnChainValue(name: String) = onChainBoxMap(name)(onChainDataInputs, onChainInputs)

  addDeclaration(height(currentHeight))
}
