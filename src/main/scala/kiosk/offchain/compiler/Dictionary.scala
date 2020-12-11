package kiosk.offchain.compiler

import kiosk.ergo.KioskType
import kiosk.offchain.compiler.model.{DataType, InputType}

import java.util.UUID
import scala.collection.mutable.{Map => MMap}

class Dictionary(currentHeight: Int) {
  private val dict = MMap[String, DictionaryObject]()
  private val lazyRefs = MMap[String, Seq[Variable]]()

  private var onChainAuxInputs = Seq[(UUID, OnChainBox)]()
  private var onChainInputs = Seq[(UUID, OnChainBox)]()
  private var onChainDataInputs = Seq[(UUID, OnChainBox)]()
  private val onChainBoxMap: MMap[String, (Seq[(UUID, OnChainBox)], Seq[(UUID, OnChainBox)], Seq[(UUID, OnChainBox)]) => KioskType[_]] = MMap()

  def getInputNanoErgs =
    onChainInputs
      .map(_._2)
      .map(_.nanoErgs.value)
      .sum

  def getInputTokens =
    onChainInputs
      .map(_._2)
      .flatMap(input => input.stringTokenIds zip input.tokenAmounts.map(_.value))
      .groupBy(_._1)
      .map { case (id, seq) => (id, seq.map(_._2).sum) }
      .toSeq

  def getInputBoxIds =
    onChainInputs
      .map(_._2)
      .map(_.boxId.toString)

  def getDataInputBoxIds =
    onChainDataInputs
      .map(_._2)
      .map(_.boxId.toString)

  def getAuxInputBoxIds =
    onChainAuxInputs
      .map(_._2)
      .map(_.boxId.toString)

  private[compiler] def getDeclaration(name: String) = dict(name).declaration

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

  def commit() = {
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

  private def getBoxes(inputType: InputType.Type, auxInputs: Seq[(UUID, OnChainBox)], dataInputs: Seq[(UUID, OnChainBox)], inputs: Seq[(UUID, OnChainBox)]) = inputType match {
    case InputType.Aux  => auxInputs
    case InputType.Data => dataInputs
    case InputType.Code => inputs
  }

//  def addOnChainDeclaration(variable: Variable, inputType: InputType.Type, mapping: Seq[OnChainBox] => KioskType[_]) = {
  def addOnChainDeclaration(variable: Variable, inputType: InputType.Type, mapping: Map[UUID, OnChainBox] => KioskType[_]) = {
    addDeclaration(OnChain(variable.name, variable.`type`))
    addOnChainBoxMapping(variable.name, (aux, data, code) => mapping(getBoxes(inputType, aux, data, code).toMap))
  }

  private def addOnChainBoxMapping(name: String, f: (Seq[(UUID, OnChainBox)], Seq[(UUID, OnChainBox)], Seq[(UUID, OnChainBox)]) => KioskType[_]) = onChainBoxMap += name -> f

  def addInput(onChainBox: OnChainBox, uuid: UUID) = onChainInputs :+= (uuid -> onChainBox)

  def addDataInput(onChainBox: OnChainBox, uuid: UUID) = onChainDataInputs :+= (uuid -> onChainBox)

  def addAuxInput(onChainBox: OnChainBox, uuid: UUID) = onChainAuxInputs :+= (uuid -> onChainBox)

  def getOnChainValue(name: String) = onChainBoxMap(name)(onChainAuxInputs, onChainDataInputs, onChainInputs)

  addDeclaration(height(currentHeight))
}
