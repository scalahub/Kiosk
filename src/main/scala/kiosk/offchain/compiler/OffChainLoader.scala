package kiosk.offchain.compiler

import kiosk.offchain.compiler.model._

import java.util.UUID

class OffChainLoader(implicit dictionary: Dictionary) {
  def load(p: Protocol): Unit = {
    (optSeq(p.constants) ++ optSeq(p.unaryOps) ++ optSeq(p.binaryOps) ++ optSeq(p.conversions) ++ optSeq(p.branches)).foreach(dictionary.addDeclaration)
    optSeq(p.auxInputUuids).foreach { case (input, uuid)  => loadInput(input, uuid, InputType.Aux) }
    optSeq(p.dataInputUuids).foreach { case (input, uuid) => loadInput(input, uuid, InputType.Data) }
    p.inputUuids.foreach { case (input, uuid)             => loadInput(input, uuid, InputType.Code) }
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

  private def loadInput(input: Input, uuid: UUID, inputType: InputType.Type): Unit = {
    input.id.foreach { id =>
      id.onChainVariable.foreach(dictionary.addOnChainDeclaration(_, inputType, _(uuid).boxId))
      dictionary.addDeclarationLazily(id)
    }
    input.address.foreach { ergoTree =>
      ergoTree.onChainVariable.foreach(dictionary.addOnChainDeclaration(_, inputType, _(uuid).address))
      dictionary.addDeclarationLazily(ergoTree)
    }
    optSeq(input.registers).foreach(register => loadRegister(register, uuid, inputType))
    optSeq(input.tokens).foreach(token => loadToken(token, uuid, inputType))
    input.nanoErgs.foreach { long =>
      long.onChainVariable.foreach(dictionary.addOnChainDeclaration(_, inputType, _(uuid).nanoErgs))
      dictionary.addDeclarationLazily(long)
    }
    dictionary.commit
  }

  private def loadRegister(register: Register, uuid: UUID, inputType: InputType.Type): Unit = {
    register.onChainVariable.map(dictionary.addOnChainDeclaration(_, inputType, _(uuid).registers(RegNum.getIndex(register.num))))
    dictionary.addDeclarationLazily(register)
  }

  private def loadToken(token: Token, uuid: UUID, inputType: InputType.Type): Unit = {
    def noIndexError = throw new Exception(s"Either token.index or token.id.value must be defined in $token")
    def getIndexForAmount(inputs: Map[UUID, OnChainBox]) = token.index.getOrElse {
      val id = token.id.getOrElse(noIndexError)
      id.value.map(_ => inputs(uuid).stringTokenIds.indexOf(id.getValue.toString)).getOrElse(noIndexError)
    }
    token.id.foreach { id =>
      id.onChainVariable.map(dictionary.addOnChainDeclaration(_, inputType, inputs => inputs(uuid).tokenIds(token.index.getOrElse(noIndexError))))
      dictionary.addDeclarationLazily(id)
    }
    token.amount.foreach { amount =>
      amount.onChainVariable.map(dictionary.addOnChainDeclaration(_, inputType, inputs => inputs(uuid).tokenAmounts(getIndexForAmount(inputs))))
      dictionary.addDeclarationLazily(amount)
    }
  }
}
