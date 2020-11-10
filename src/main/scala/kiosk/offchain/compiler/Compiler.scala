package kiosk.offchain.compiler

import kiosk.offchain.model._

object Compiler {
  def compile(protocol: Protocol) = {
    implicit val dictionary = new Dictionary
    val single: Option[Boolean] = Some(false)
    val inputs: Seq[(Input, Option[Boolean])] = (protocol.dataInputs.toSeq.flatten ++ protocol.inputs.toSeq.flatten).map(input => (input, Some(input.isMulti)))
    val declarations = protocol.constants.toSeq.flatten.map((_, single)) ++
      protocol.unaryOps.toSeq.flatten.map((_, None)) ++
      protocol.binaryOps.toSeq.flatten.map((_, None)) ++
      protocol.conversions.toSeq.flatten.map((_, None)) ++
      inputs.flatMap(input => addInput(input._1, input._2)) ++
      protocol.outputs.toSeq.flatten.flatMap(addOutput)

    declarations.foreach {
      case (declaration, isMulti) => dictionary.addDeclaration(declaration, isMulti)
    }
    println("\n== USED ==")
    dictionary.print(isLazy = false)
    println("\n== UNUSED ==")
    dictionary.print(isLazy = true)
  }

  def addInput(input: Input, isMulti: Option[Boolean]): Seq[(Declaration, Option[Boolean])] = {
    input.boxId.toSeq.map((_, isMulti)) ++
      input.address.toSeq.map((_, isMulti)) ++
      input.registers.toSeq.flatten.map((_, isMulti)) ++
      input.tokens.toSeq.flatten.flatMap(token => addToken(token, isMulti)) ++
      input.nanoErgs.toSeq.map((_, isMulti)) ++
      input.boxCount.toSeq.map((_, isMulti))
  }

  def addOutput(output: Output): Seq[(Declaration, Option[Boolean])] = {
    Seq((output.address, None)) ++
      output.registers.toSeq.flatten.map((_, None)) ++
      output.tokens.toSeq.flatten.flatMap(token => addToken(token, None)) ++
      Seq((output.nanoErgs, None))
  }

  def addToken(token: Token, isMulti: Option[Boolean]): Seq[(Declaration, Option[Boolean])] = {
    token.id.toSeq.map((_, isMulti)) ++
      token.numTokens.toSeq.map((_, isMulti))
  }
}
