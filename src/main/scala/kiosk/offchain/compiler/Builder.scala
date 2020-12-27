package kiosk.offchain.compiler

import kiosk.ergo
import kiosk.ergo.{KioskBox, KioskType}
import kiosk.offchain.compiler.model.{DataType, Output, Protocol, RegNum}

class Builder(implicit dictionary: Dictionary) {
  def buildOutputs(protocol: Protocol) = protocol.outputs.flatMap(createOutput).filter(_.value > 0)

  private def createOutput(output: Output): Seq[KioskBox] = {
    type OutputRegister = KioskType[_]
    type OutputAddress = String
    type OutputToken = (String, Long)

    val addresses: Multiple[OutputAddress] = output.address.getValues.map(tree2str)

    val registers: Multiple[Seq[OutputRegister]] = Multiple.sequence(
      optSeq(output.registers)
        .map { register =>
          val index: Int = RegNum.getIndex(register.num)
          val regType: DataType.Type = register.`type`

          val values: Multiple[KioskType[_]] = register.getValues
          values.foreach(value => require(DataType.isValid(value, regType), s"Invalid type ${value.typeName} for register ${register.num}. Expected $regType"))
          (index, values)
        }
        .sortBy(_._1)
        .ensuring(noGapsInIndices(_), s"Register should start from R4 and must not have gaps")
        .map(_._2)
    )

    val tokens: Multiple[Seq[OutputToken]] = Multiple.sequence(
      optSeq(output.tokens)
        .map { token =>
          val index: Int = token.index.getOrElse(throw new Exception("Token index must be defined in output"))
          val id: model.Id = token.id.getOrElse(throw new Exception("Token id must be defined in output"))
          val amount: model.Long = token.amount.getOrElse(throw new Exception("Token amount must be defined in output"))
          (index, id.getValues.map(_.toString) zip amount.getValues.map(_.value))
        }
        .filter {
          case (_, multipleToken) =>
            multipleToken.forall {
              case (_, amount) => amount > 0
            }
        }
        .sortBy(_._1)
        .ensuring(noGapsInIndices(_), s"Token indices should start from 0 and must not have gaps")
        .map(_._2)
    )

    val nanoErgs: Multiple[ergo.KioskLong] = output.nanoErgs.getValues.ensuring(_.forall(_.value > 0), s"One or more outputs will have non-positive nano-Ergs: $output")

    val generatedOutputs = (addresses zip nanoErgs zip registers zip tokens).seq map {
      case (((outputAddress, outputNanoErgs), outputRegisters), outputTokens) => KioskBox(outputAddress, outputNanoErgs.value, outputRegisters.toArray, outputTokens.toArray)
    }
    val outputsToReturn = if (output.multi) generatedOutputs else generatedOutputs.take(1)

    if (output.optional || outputsToReturn.nonEmpty) outputsToReturn else throw new Exception(s"Output declaration generated zero boxes (use 'Optional' flag to prevent this error): $output")
  }
}
