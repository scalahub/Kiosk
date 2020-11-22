package kiosk.offchain.compiler

import kiosk.offchain.model.{Input, Output, Protocol, RegNum, Register, Token}

object Loader {
  def load(p: Protocol)(implicit dictionary: Dictionary): Unit = {
    (optSeq(p.constants) ++ optSeq(p.unaryOps) ++ optSeq(p.binaryOps) ++ optSeq(p.conversions)).foreach(dictionary.addDeclaration)
    optSeq(p.dataInputs).zipWithIndex.foreach { case (input, index) => addDeclarations(input, index, true) }
    p.inputs.zipWithIndex.foreach { case (input, index)             => addDeclarations(input, index, false) }
    p.outputs.foreach(addDeclarations(_))
  }

  private def addDeclarations(output: Output)(implicit dictionary: Dictionary): Unit = {
    dictionary.addDeclarationLazily(output.address)
    output.registers.toSeq.flatten.foreach(register => dictionary.addDeclarationLazily(register))
    output.tokens.toSeq.flatten.foreach { outToken =>
      dictionary.addDeclarationLazily(outToken.id)
      dictionary.addDeclarationLazily(outToken.amount)
    }
    dictionary.addDeclarationLazily(output.nanoErgs)
    dictionary.commit
  }

  private def addDeclarations(input: Input, inputIndex: Int, isDataInput: Boolean)(implicit dictionary: Dictionary): Unit = {
    input.id.foreach { id =>
      id.onChainVariable.map { variable =>
        dictionary.addDeclaration(OnChainConstant(variable.name, variable.`type`))
        dictionary.addOnChainBoxMapping(variable.name, (dataInputs, inputs) => (if (isDataInput) dataInputs else inputs)(inputIndex).boxId)
      }
      dictionary.addDeclarationLazily(id)
    }
    input.address.foreach { ergoTree =>
      ergoTree.onChainVariable.map { variable =>
        dictionary.addDeclaration(OnChainConstant(variable.name, variable.`type`))
        dictionary.addOnChainBoxMapping(variable.name, (dataInputs, inputs) => (if (isDataInput) dataInputs else inputs)(inputIndex).address)
      }
      dictionary.addDeclarationLazily(ergoTree)
    }
    input.registers.toSeq.flatten.foreach(register => addDeclarations(register, inputIndex, isDataInput))
    input.tokens.toSeq.flatten.foreach(token => addDeclarations(token, inputIndex, isDataInput))
    input.nanoErgs.foreach { long =>
      long.onChainVariable.map { variable =>
        dictionary.addDeclaration(OnChainConstant(variable.name, variable.`type`))
        dictionary.addOnChainBoxMapping(variable.name, (dataInputs, inputs) => (if (isDataInput) dataInputs else inputs)(inputIndex).nanoErgs)
      }
      dictionary.addDeclarationLazily(long)
    }
    dictionary.commit
  }

  private def addDeclarations(register: Register, inputIndex: Int, isDataInput: Boolean)(implicit dictionary: Dictionary): Unit = {
    register.onChainVariable.map { variable =>
      dictionary.addDeclaration(OnChainConstant(variable.name, variable.`type`))
      dictionary.addOnChainBoxMapping(variable.name, (dataInputs, inputs) => (if (isDataInput) dataInputs else inputs)(inputIndex).registers(RegNum.getIndex(register.num)))
    }
    dictionary.addDeclarationLazily(register)
  }

  private def addDeclarations(token: Token, inputIndex: Int, isDataInput: Boolean)(implicit dictionary: Dictionary): Unit = {
    token.id.onChainVariable.map { variable =>
      dictionary.addDeclaration(OnChainConstant(variable.name, variable.`type`))
      dictionary.addOnChainBoxMapping(variable.name, (dataInputs, inputs) => (if (isDataInput) dataInputs else inputs)(inputIndex).tokenIds(token.index))
    }
    dictionary.addDeclarationLazily(token.id)
    token.amount.onChainVariable.map { variable =>
      dictionary.addDeclaration(OnChainConstant(variable.name, variable.`type`))
      dictionary.addOnChainBoxMapping(variable.name, (dataInputs, inputs) => (if (isDataInput) dataInputs else inputs)(inputIndex).tokenAmounts(token.index))
    }
    dictionary.addDeclarationLazily(token.amount)
  }

}
