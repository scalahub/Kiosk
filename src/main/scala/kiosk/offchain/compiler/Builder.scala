package kiosk.offchain.compiler

import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo
import kiosk.ergo.{KioskBox, KioskCollByte, KioskErgoTree, KioskLong}
import kiosk.offchain.model.{DataType, Output, Protocol, RegNum}
import org.ergoplatform.ErgoAddress

object Builder {
  def buildOutputs(protocol: Protocol)(dictionary: Dictionary) = {
    protocol.outputs.map(createOutput(_)(dictionary))
  }

  private def noGaps(sorted: Seq[(Int, _)]): Boolean = {
    sorted.map(_._1).zipWithIndex.forall { case (int, index) => int == index }
  }

  private def createOutput(output: Output)(dictionary: Dictionary): KioskBox = {
    val ergoAddress: ErgoAddress = ScalaErgoConverters.getAddressFromErgoTree(dictionary.getValue(output.address.value.get).asInstanceOf[KioskErgoTree].value)
    val address: String = ScalaErgoConverters.getStringFromAddress(ergoAddress)
    val registers: Seq[ergo.KioskType[_]] = optSeq(output.registers)
      .map { register =>
        val value: ergo.KioskType[_] = dictionary.getValue(register.value.get)
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
        val tokenId: KioskCollByte = dictionary.getValue(token.id.value.get).asInstanceOf[KioskCollByte]
        val index: Int = token.index
        val amount: KioskLong = dictionary.getValue(token.amount.value.get).asInstanceOf[KioskLong]
        (index, (tokenId.toString, amount.value))
      }
      .sortBy(_._1)
      .ensuring(noGaps(_), s"Token indices should start from 0 and must not have gaps")
      .map(_._2)
    val nanoErgs: scala.Long = dictionary.getValue(output.nanoErgs.value.get).asInstanceOf[KioskLong].value
    KioskBox(address, nanoErgs, registers.toArray, tokens.toArray)
  }
}
