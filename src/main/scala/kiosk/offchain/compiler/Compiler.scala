package kiosk.offchain.compiler

import kiosk.offchain.model._

object Compiler {
  def printObject(key: String, dictionaryObject: DictionaryObject) = println(s"$key: ${dictionaryObject.declaration.`type`}")

  def printObjects(objects: Seq[(String, DictionaryObject)]) = objects.map { case (key, dictionaryObject) => printObject(key, dictionaryObject) }

  def printValues(objects: Seq[(String, DictionaryObject)], dictionary: Dictionary) = objects.foreach {
    case (_, value) => println(s"${value.declaration} = ${value.declaration.getValue(dictionary)}")
  }

  def printDictionary(dictionary: Dictionary) = {
    val used: Seq[(String, DictionaryObject)] = dictionary.getDictionaryObjects(unresolved = false)
    val unused: Seq[(String, DictionaryObject)] = dictionary.getDictionaryObjects(unresolved = true)
    println("\n== Unused ==")
    printObjects(unused)
    println("\n== Used ==")
    printObjects(used)
    // println("\n== Values ==")
    // printValues(used, dictionary)
  }

  def compile(protocol: Protocol) = {
    implicit val dictionary = new Dictionary
    optSeq(protocol.constants).map(value => value.getValue(dictionary))
    addDeclarations(protocol)(dictionary)
    ////////////////////////////
    //

    printDictionary(dictionary)
  }

  private def optSeq[T](s: Option[Seq[T]]) = s.toSeq.flatten

  private def addDeclarations(p: Protocol)(dictionary: Dictionary): Unit = {
    (optSeq(p.constants) ++ optSeq(p.unaryOps) ++ optSeq(p.binaryOps) ++ optSeq(p.conversions)).foreach(dictionary.addDeclaration)
    optSeq(p.dataInputs).zipWithIndex.foreach { case (input, index) => addDeclarations(input, index, true)(dictionary) }
    p.inputs.zipWithIndex.foreach { case (input, index)             => addDeclarations(input, index, false)(dictionary) }
    p.outputs.foreach(addDeclarations(_)(dictionary))
    p.fee.toSeq.foreach(dictionary.addDeclaration)
  }

  private def addDeclarations(output: Output)(dictionary: Dictionary): Unit = {
    dictionary.addDeclaration(output.address)
    output.registers.toSeq.flatten.foreach(register => dictionary.addDeclaration(register))
    output.tokens.toSeq.flatten.foreach { token =>
      dictionary.addDeclaration(token.tokenId)
      dictionary.addDeclaration(token.amount)
    }
    dictionary.addDeclaration(output.nanoErgs)
  }

  private def addDeclarations(input: Input, inputIndex: Int, isDataInput: Boolean)(implicit dictionary: Dictionary): Unit = {
    input.boxId.map { id =>
      id.onChainVariable.map { variable =>
        dictionary.addDeclaration(OnChainConstant(variable.name, variable.`type`))
        dictionary.addRealBoxMapping(variable.name, (dataInputs, inputs) => (if (isDataInput) dataInputs else inputs)(inputIndex).boxId)
      }
      dictionary.addDeclaration(id)
    }
    input.address.map { ergoTree =>
      ergoTree.onChainVariable.map { variable =>
        dictionary.addDeclaration(OnChainConstant(variable.name, variable.`type`))
        dictionary.addRealBoxMapping(variable.name, (dataInputs, inputs) => (if (isDataInput) dataInputs else inputs)(inputIndex).address)
      }
      dictionary.addDeclaration(ergoTree)
    }
    input.registers.toSeq.flatten.map(register => addDeclarations(register, inputIndex, isDataInput))
    input.tokens.toSeq.flatten.map(token => addDeclarations(token, inputIndex, isDataInput))
    input.nanoErgs.map { long =>
      long.onChainVariable.map { variable =>
        dictionary.addDeclaration(OnChainConstant(variable.name, variable.`type`))
        dictionary.addRealBoxMapping(variable.name, (dataInputs, inputs) => (if (isDataInput) dataInputs else inputs)(inputIndex).nanoErgs)
      }
      dictionary.addDeclaration(long)
    }
  }

  private def addDeclarations(register: Register, inputIndex: Int, isDataInput: Boolean)(implicit dictionary: Dictionary): Unit = {
    register.onChainVariable.map { variable =>
      dictionary.addDeclaration(OnChainConstant(variable.name, variable.`type`))
      dictionary.addRealBoxMapping(variable.name, (dataInputs, inputs) => (if (isDataInput) dataInputs else inputs)(inputIndex).registers(RegNum.getIndex(register.num)))
    }
    dictionary.addDeclaration(register)
  }

  private def addDeclarations(token: Token, inputIndex: Int, isDataInput: Boolean)(implicit dictionary: Dictionary): Unit = {
    token.tokenId.onChainVariable.map { variable =>
      dictionary.addDeclaration(OnChainConstant(variable.name, variable.`type`))
      dictionary.addRealBoxMapping(variable.name, (dataInputs, inputs) => (if (isDataInput) dataInputs else inputs)(inputIndex).tokenIds(token.index))
    }
    dictionary.addDeclaration(token.tokenId)
    token.amount.onChainVariable.map { variable =>
      dictionary.addDeclaration(OnChainConstant(variable.name, variable.`type`))
      dictionary.addRealBoxMapping(variable.name, (dataInputs, inputs) => (if (isDataInput) dataInputs else inputs)(inputIndex).tokenAmounts(token.index))
    }
    dictionary.addDeclaration(token.amount)
  }
}
