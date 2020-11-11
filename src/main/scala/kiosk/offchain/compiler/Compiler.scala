package kiosk.offchain.compiler

import kiosk.offchain.model._

object Compiler {
  private def fromOptSeq(s: Option[Seq[Declaration]], isMulti: Option[Boolean]): Seq[(Declaration, Option[Boolean])] = {
    s.toSeq.flatten.map((_, isMulti))
  }

  private def fromOpt(s: Option[Declaration], isMulti: Option[Boolean]): Seq[(Declaration, Option[Boolean])] = {
    s.toSeq.map((_, isMulti))
  }

  private def toSeqT[T](t: Option[Seq[T]]) = t.toSeq.flatten

  def compile(protocol: Protocol) = {
    implicit val dictionary = new Dictionary

    val declarations: Seq[(Declaration, Option[Boolean])] =
      fromOptSeq(protocol.constants, Some(false)) ++
        fromOptSeq(protocol.unaryOps, None) ++
        fromOptSeq(protocol.binaryOps, None) ++
        fromOptSeq(protocol.conversions, None) ++
        toSeqT(protocol.dataInputs).flatMap(addInput) ++
        toSeqT(protocol.inputs).flatMap(addInput) ++
        toSeqT(protocol.outputs).flatMap(addOutput)

    declarations.foreach {
      case (declaration, isMulti) =>
        println(s"Adding declaration ${declaration.name}: ${declaration.`type`} -> ${declaration.refs}")
        dictionary.addDeclaration(declaration, isMulti)
    }
    println("\n== USED ==")
    dictionary.print(isLazy = false)
    println("\n== UNUSED ==")
    dictionary.print(isLazy = true)
  }

  def addInput(input: Input): Seq[(Declaration, Option[Boolean])] = {
    val optionMulti = Some(input.boxCount.isDefined)

    fromOpt(input.boxId, optionMulti) ++
      fromOpt(input.address, optionMulti) ++
      fromOptSeq(input.registers, optionMulti) ++
      toSeqT(input.tokens).flatMap(addToken(_, optionMulti)) ++
      fromOpt(input.nanoErgs, optionMulti) ++
      fromOpt(input.boxCount, optionMulti)
  }

  def addOutput(output: Output): Seq[(Declaration, Option[Boolean])] = {
    val optionMulti = Some(output.boxCount.isDefined)

    Seq((output.address, optionMulti)) ++
      fromOptSeq(output.registers, optionMulti) ++
      toSeqT(output.tokens).flatMap(addToken(_, optionMulti)) ++
      Seq((output.nanoErgs, optionMulti)) ++
      fromOpt(output.boxCount, optionMulti)
  }

  def addToken(token: Token, isMulti: Option[Boolean]): Seq[(Declaration, Option[Boolean])] = {
    fromOpt(token.id, isMulti) ++ fromOpt(token.numTokens, isMulti)
  }
}
