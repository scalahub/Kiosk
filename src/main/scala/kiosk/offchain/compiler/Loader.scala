package kiosk.offchain.compiler

import kiosk.offchain.compiler.model._

class Loader(implicit dictionary: Dictionary) {
  def load(p: Protocol): Unit = {
    (optSeq(p.constants) ++ optSeq(p.unaryOps) ++ optSeq(p.binaryOps) ++ optSeq(p.conversions)).foreach(dictionary.addDeclaration)
    optSeq(p.dataInputs).zipWithIndex.foreach { case (input, index) => loadInput(input, index, true) }
    p.inputs.zipWithIndex.foreach { case (input, index)             => loadInput(input, index, false) }
    p.outputs.foreach(loadOutput)
  }

  private def loadOutput(output: Output): Unit = {
    dictionary.addDeclarationLazily(output.address)
    optSeq(output.registers).foreach(register => dictionary.addDeclarationLazily(register))
    optSeq(output.tokens).foreach { outToken =>
      outToken.id.foreach(dictionary.addDeclarationLazily)
      outToken.amount.foreach(dictionary.addDeclarationLazily)
    }
    dictionary.addDeclarationLazily(output.nanoErgs)
    dictionary.commit
  }

  private def loadInput(input: Input, index: Int, isDataInput: Boolean): Unit = {
    input.id.foreach { id =>
      id.onChainVariable.foreach(dictionary.addOnChainDeclaration(_, isDataInput, _(index).boxId))
      dictionary.addDeclarationLazily(id)
    }
    input.address.foreach { ergoTree =>
      ergoTree.onChainVariable.foreach(dictionary.addOnChainDeclaration(_, isDataInput, _(index).address))
      dictionary.addDeclarationLazily(ergoTree)
    }
    optSeq(input.registers).foreach(register => loadRegister(register, index, isDataInput))
    optSeq(input.tokens).foreach(token => loadToken(token, index, isDataInput))
    input.nanoErgs.foreach { long =>
      long.onChainVariable.foreach(dictionary.addOnChainDeclaration(_, isDataInput, _(index).nanoErgs))
      dictionary.addDeclarationLazily(long)
    }
    dictionary.commit
  }

  private def loadRegister(register: Register, inputIndex: Int, isDataInput: Boolean): Unit = {
    register.onChainVariable.map(dictionary.addOnChainDeclaration(_, isDataInput, _(inputIndex).registers(RegNum.getIndex(register.num))))
    dictionary.addDeclarationLazily(register)
  }

  private def loadToken(token: Token, inputIndex: Int, isDataInput: Boolean): Unit = {
    def noIndexError = throw new Exception(s"Either token.index or token.id.value must be defined in $token")
    def getIndexForAmount(inputs: Seq[OnChainBox]) = token.index.getOrElse {
      val id = token.id.getOrElse(noIndexError)
      id.value.map(_ => inputs(inputIndex).stringTokenIds.indexOf(id.getValue.toString)).getOrElse(noIndexError)
    }
    token.id.foreach { id =>
      id.onChainVariable.map(dictionary.addOnChainDeclaration(_, isDataInput, inputs => inputs(inputIndex).tokenIds(token.index.getOrElse(noIndexError))))
      dictionary.addDeclarationLazily(id)
    }
    token.amount.foreach { amount =>
      amount.onChainVariable.map(dictionary.addOnChainDeclaration(_, isDataInput, inputs => inputs(inputIndex).tokenAmounts(getIndexForAmount(inputs))))
      dictionary.addDeclarationLazily(amount)
    }
  }
}
