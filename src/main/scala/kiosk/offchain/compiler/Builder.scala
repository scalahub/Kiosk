package kiosk.offchain.compiler

import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo
import kiosk.ergo.{KioskBox, KioskErgoTree}
import kiosk.offchain.compiler.model.{DataType, Output, Protocol, RegNum}
import org.ergoplatform.ErgoAddress

class Builder(implicit dictionary: Dictionary) {
  def buildOutputs(protocol: Protocol) = {
    protocol.outputs.map(createOutput)
  }

  private def noGaps(sorted: Seq[(Int, _)]): Boolean = {
    sorted.map(_._1).zipWithIndex.forall { case (int, index) => int == index }
  }

  private def createOutput(output: Output): KioskBox = {
    val ergoTree = output.address.getValue
    val ergoAddress: ErgoAddress = ScalaErgoConverters.getAddressFromErgoTree(ergoTree.value)
    val address: String = ScalaErgoConverters.getStringFromAddress(ergoAddress)
    val registers: Seq[ergo.KioskType[_]] = optSeq(output.registers)
      .map { register =>
        val value: ergo.KioskType[_] = register.getValue
        val index: Int = RegNum.getIndex(register.num)
        val regType: DataType.Type = register.`type`
        require(DataType.isValid(value, regType), s"Invalid type ${value.typeName} for register ${register.num}. Expected $regType")
        (index, value)
      }
      .sortBy(_._1)
      .ensuring(noGaps(_), s"Register should start from R4 and must not have gaps")
      .map(_._2)

    val tokens: Seq[(String, scala.Long)] = optSeq(output.tokens)
      .map { token =>
        val index: Int = token.index.getOrElse(throw new Exception("Token index must be defined in output"))
        val id = token.id.getOrElse(throw new Exception("Token id must be defined in output"))
        val amount = token.amount.getOrElse(throw new Exception("Token amount must be defined in output"))
        (index, (id.getValue.toString, amount.getValue.value))
      }
      .sortBy(_._1)
      .ensuring(noGaps(_), s"Token indices should start from 0 and must not have gaps")
      .map(_._2)
    KioskBox(address, output.nanoErgs.getValue.value, registers.toArray, tokens.toArray)
  }
}
