package kiosk.offchain.compiler

import kiosk.offchain.model._

import scala.collection.mutable

object Compiler {
  def printDictionaryObject(key: String, dictionaryObject: DictionaryObject) = {
    println(s"$key: ${dictionaryObject.declaration.`type`} = ${dictionaryObject.value.map(_.hex).getOrElse("")}")
  }
  def printDictionary(dictionary: mutable.Map[String, DictionaryObject]) = {
    dictionary.map { case (key, dictionaryObject) => printDictionaryObject(key, dictionaryObject) }
  }

  def print(dictionary: Dictionary) = {
    println("\n== Used ==")
    printDictionary(dictionary.getDictionaryObjects(unresolved = false))
    println("\n== Unused ==")
    printDictionary(dictionary.getDictionaryObjects(unresolved = false))
  }

  def compile(protocol: Protocol) = {
    implicit val dictionary = new Dictionary
    getDeclarations(protocol).foreach(dictionary.addDeclaration)
    print(dictionary)
  }

  private def optSeq[T](s: Option[Seq[T]]) = s.toSeq.flatten

  private def opt[T](s: Option[T]) = s.toSeq

  private def getDeclarations(p: Protocol): Seq[Declaration] = {
    /*
       Order of evaluation (resolution of variables):
         constants
         dataInputs
         inputs
         outputs,
         fee
         binaryOps/unaryOps/conversions (lazy)

         This means a variable defined in inputs can be referenced in dataInputs but not vice-versa
         Similarly a variable defined in first input can be referenced in the second input but not vice-versa

         lazy variables are not evaluated until needed
     */

    optSeq(p.constants) ++ optSeq(p.unaryOps) ++ optSeq(p.binaryOps) ++ optSeq(p.conversions) ++
      optSeq(p.dataInputs).flatMap(getDeclarations) ++ optSeq(p.inputs).flatMap(getDeclarations) ++
      optSeq(p.outputs).flatMap(getDeclarations) ++ p.fee.toSeq
  }

  private def getDeclarations(input: Input): Seq[Declaration] = {
    opt(input.boxId) ++ opt(input.address) ++ optSeq(input.registers) ++ optSeq(input.tokens).flatMap(getDeclarations) ++ input.nanoErgs.toSeq
  }

  private def getDeclarations(output: Output): Seq[Declaration] = {
    Seq(output.address) ++ optSeq(output.registers) ++ optSeq(output.tokens).flatMap(getDeclarations) ++ Seq(output.nanoErgs)
  }

  private def getDeclarations(token: Token): Seq[Declaration] = {
    opt(token.tokenId) ++ token.amount.toSeq
  }
}
