package kiosk.offchain.compiler

import kiosk.offchain.model._

object Compiler {
  def compile(protocol: Protocol) = {
    implicit val dictionary = new Dictionary
    declare(protocol).foreach {
      case (declaration, optIsMulti) =>
        println(s"Adding ${declaration.name.getOrElse("_")}: ${declaration.`type`} -> ${declaration.refs}")
        dictionary.addDeclaration(declaration, optIsMulti)
    }
    println("\n== Non-Lazy ==")
    dictionary.print(isLazy = false)
    println("\n== Lazy ==")
    dictionary.print(isLazy = true)
  }

  private def fromOptSeq(s: Option[Seq[Declaration]], optIsMulti: Option[Boolean]) = s.toSeq.flatten.map((_, optIsMulti))

  private def fromOpt(s: Option[Declaration], optIsMulti: Option[Boolean]) = s.toSeq.map((_, optIsMulti))

  private def toSeq[T](t: Option[Seq[T]]) = t.toSeq.flatten

  private def declare(protocol: Protocol): Seq[(Declaration, Option[Boolean])] = {
    fromOptSeq(protocol.constants, None) ++
      fromOptSeq(protocol.unaryOps, None) ++
      fromOptSeq(protocol.binaryOps, None) ++
      fromOptSeq(protocol.conversions, None) ++
      toSeq(protocol.dataInputs).flatMap(declare) ++
      toSeq(protocol.inputs).flatMap(declare) ++
      toSeq(protocol.outputs).flatMap(declare)
  }

  private def declare(input: Input): Seq[(Declaration, Option[Boolean])] = {
    val optIsMulti = Some(input.isMulti)

    fromOpt(input.boxId, optIsMulti) ++
      fromOpt(input.address, optIsMulti) ++
      fromOptSeq(input.registers, optIsMulti) ++
      toSeq(input.tokens).flatMap(declare(_, optIsMulti)) ++
      fromOpt(input.nanoErgs, optIsMulti) ++
      fromOpt(input.boxCount, optIsMulti)
  }

  private def declare(output: Output): Seq[(Declaration, Option[Boolean])] = {
    val optIsMulti = Some(output.isMulti)

    Seq((output.address, optIsMulti)) ++
      fromOptSeq(output.registers, optIsMulti) ++
      toSeq(output.tokens).flatMap(declare(_, optIsMulti)) ++
      Seq((output.nanoErgs, optIsMulti)) ++
      fromOpt(output.boxCount, optIsMulti)
  }

  private def declare(token: Token, optIsMulti: Option[Boolean]): Seq[(Declaration, Option[Boolean])] = {
    fromOpt(token.id, optIsMulti) ++ fromOpt(token.numTokens, optIsMulti)
  }
}
