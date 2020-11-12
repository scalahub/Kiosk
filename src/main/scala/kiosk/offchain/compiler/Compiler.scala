package kiosk.offchain.compiler

import kiosk.offchain.model._

object Compiler {
  def printDictionary(implicit dictionary: Dictionary) = {
    println("\n== Used ==")
    dictionary.print(unresolved = false)
    println("\n== Unused ==")
    dictionary.print(unresolved = true)
  }
  def compile(protocol: Protocol) = {
    implicit val dictionary = new Dictionary
    declareProtocol(protocol).foreach(dictionary.addDeclaration)
    printDictionary
  }

  private def optSeq[T](s: Option[Seq[T]]) = s.toSeq.flatten

  private def opt[T](s: Option[T]) = s.toSeq

  private def declareProtocol(p: Protocol): Seq[Declaration] = {
    optSeq(p.constants) ++ optSeq(p.unaryOps) ++ optSeq(p.binaryOps) ++ optSeq(p.conversions) ++
      optSeq(p.dataInputs).flatMap(declareInput) ++ optSeq(p.inputs).flatMap(declareInput) ++
      optSeq(p.outputs).flatMap(declareOutput) ++ p.fee.toSeq
  }

  private def declareInput(input: Input) = {
    opt(input.boxId) ++ opt(input.address) ++ optSeq(input.registers) ++ optSeq(input.tokens).flatMap(declareToken) ++ input.nanoErgs.toSeq
  }

  private def declareOutput(output: Output) = {
    Seq(output.address) ++ optSeq(output.registers) ++ optSeq(output.tokens).flatMap(declareToken) ++ Seq(output.nanoErgs)
  }

  private def declareToken(token: Token) = {
    opt(token.id) ++ token.numTokens.toSeq
  }
}
