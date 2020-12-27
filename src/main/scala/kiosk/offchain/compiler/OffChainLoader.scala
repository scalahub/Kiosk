package kiosk.offchain.compiler

import kiosk.offchain.compiler.model._

import java.util.UUID

class OffChainLoader(implicit dictionary: Dictionary) {
  def load(p: Protocol): Unit = {
    (optSeq(p.constants) ++ optSeq(p.unaryOps) ++ optSeq(p.binaryOps) ++ optSeq(p.conversions) ++ optSeq(p.branches)).foreach(dictionary.addDeclaration)
    optSeq(p.auxInputUuids).zipWithIndex.foreach { case ((input, uuid), index)  => loadInput(input)(uuid, index, InputType.Aux) }
    optSeq(p.dataInputUuids).zipWithIndex.foreach { case ((input, uuid), index) => loadInput(input)(uuid, index, InputType.Data) }
    p.inputUuids.zipWithIndex.foreach { case ((input, uuid), index)             => loadInput(input)(uuid, index, InputType.Code) }
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

  private def noBoxError(implicit inputType: InputType.Type, inputIndex: Int) =
    throw new Exception(s"No $inputType-input matched at ${MatchingOptions.Optional} index $inputIndex when getting target")

  private def getInput(mapping: Map[UUID, Multiple[OnChainBox]])(implicit uuid: UUID, inputType: InputType.Type, index: Int) = mapping.get(uuid).getOrElse(noBoxError)

  private def loadInput(input: Input)(implicit inputUuid: UUID, inputIndex: Int, inputType: InputType.Type): Unit = {
    input.id.foreach { id =>
      id.onChainVariable.foreach(dictionary.addOnChainDeclaration(_, inputType, getInput(_).map(_.boxId)))
      dictionary.addDeclarationLazily(id)
    }
    input.address.foreach { ergoTree =>
      ergoTree.onChainVariable.foreach(dictionary.addOnChainDeclaration(_, inputType, getInput(_).map(_.address)))
      dictionary.addDeclarationLazily(ergoTree)
    }
    optSeq(input.registers).foreach(loadRegister)
    optSeq(input.tokens).foreach(loadToken)
    input.nanoErgs.foreach { long =>
      long.onChainVariable.foreach(dictionary.addOnChainDeclaration(_, inputType, getInput(_).map(_.nanoErgs)))
      dictionary.addDeclarationLazily(long)
    }
    dictionary.commit
  }

  private def loadRegister(register: Register)(implicit inputUuid: UUID, inputIndex: Int, inputType: InputType.Type): Unit = {
    register.onChainVariable.map(dictionary.addOnChainDeclaration(_, inputType, getInput(_).map(_.registers(RegNum.getIndex(register.num)))))
    dictionary.addDeclarationLazily(register)
  }

  private def loadToken(token: Token)(implicit inputUuid: UUID, inputIndex: Int, inputType: InputType.Type): Unit = {
    def noIndexError = throw new Exception(s"Either token.index or token.id.value must be defined in $token")
    def getIndicesForAmount(inputs: Map[UUID, Multiple[OnChainBox]]): Multiple[Int] = {
      val onChainBoxes = inputs(inputUuid)
      token.index.fold {
        val id = token.id.getOrElse(noIndexError)
        id.value.fold(noIndexError)(_ => onChainBoxes.map(_.stringTokenIds.indexOf(id.getValues.toString)))
      }(int => onChainBoxes.map(_ => int))
    }
    token.id.foreach { id =>
      id.onChainVariable.map(dictionary.addOnChainDeclaration(_, inputType, getInput(_).map(_.tokenIds(token.index.getOrElse(noIndexError)))))
      dictionary.addDeclarationLazily(id)
    }
    token.amount.foreach { amount =>
      amount.onChainVariable.map(dictionary.addOnChainDeclaration(_, inputType, inputs => (getInput(inputs) zip getIndicesForAmount(inputs)) map { case (input, index) => input.tokenAmounts(index) }))
      dictionary.addDeclarationLazily(amount)
    }
  }
}
