package kiosk.offchain.reader

import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo._
import kiosk.explorer.Explorer
import kiosk.offchain.compiler.model._
import kiosk.offchain.compiler.{Dictionary, OnChainBox, model, optSeq}
import org.ergoplatform.ErgoAddress
import sigmastate.Values

class Reader(implicit dictionary: Dictionary) {
  def getBoxes(input: Input, alreadySelectedBoxIds: Seq[String]): Seq[OnChainBox] = {
    val boxById = for {
      id <- input.id
      _ <- id.value
    } yield {
      val boxId: String = id.getValue.toString
      OnChainBox.fromKioskBox(Explorer.getBoxById(boxId))
    }

    val boxesByAddress = for {
      address <- input.address
      _ <- address.value
    } yield {
      val ergoTree: Values.ErgoTree = address.getValue.asInstanceOf[KioskErgoTree].value
      val ergoAddress: ErgoAddress = ScalaErgoConverters.getAddressFromErgoTree(ergoTree)
      val stringAddress: String = ScalaErgoConverters.getStringFromAddress(ergoAddress)
      Explorer.getUnspentBoxes(stringAddress).map(OnChainBox.fromKioskBox)
    }

    val matchedBoxes = optSeq(boxesByAddress) ++ boxById

    val filteredBySelected = matchedBoxes.filterNot(box => alreadySelectedBoxIds.contains(box.boxId.toString))

    val filteredByRegisters = filterByRegisters(filteredBySelected, optSeq(input.registers))

    val filteredByTokens = filterByTokens(filteredByRegisters, optSeq(input.tokens))

    val filteredByNanoErgs = for {
      nanoErgs <- input.nanoErgs
      target <- nanoErgs.getFilterTarget
    } yield filteredByTokens.filter(onChainBox => FilterOp.matches(onChainBox.nanoErgs, target, nanoErgs.filterOp))

    filteredByNanoErgs.getOrElse(filteredByTokens)
  }

  private def filterByRegisters(boxes: Seq[OnChainBox], registers: Seq[Register]): Seq[OnChainBox] = {
    registers.foldLeft(boxes)((boxesBeforeFilter, register) => filterByRegister(boxesBeforeFilter, register))
  }

  private def filterByRegister(boxes: Seq[OnChainBox], register: Register): Seq[OnChainBox] = {
    val index: Int = RegNum.getIndex(register.num)
    val filteredByType: Seq[OnChainBox] = boxes.filter(box => DataType.isValid(box.registers(index), register.`type`))

    register.value
      .map { _ =>
        val expected: KioskType[_] = register.getValue
        filteredByType.filter { box =>
          val kioskType: KioskType[_] = box.registers(index)
          kioskType.typeName == expected.typeName && kioskType.hex == expected.hex
        }
      }
      .getOrElse(filteredByType)
  }

  private def filterByTokens(boxes: Seq[OnChainBox], tokens: Seq[model.Token]): Seq[OnChainBox] = {
    tokens.foldLeft(boxes)((boxesBeforeFilter, token) => filterByToken(boxesBeforeFilter, token))
  }

  private def filterByToken(boxes: Seq[OnChainBox], token: model.Token): Seq[OnChainBox] = {
    val filteredById: Seq[OnChainBox] = token.id.value
      .map { _ =>
        val requiredTokenId: String = token.id.getValue.toString
        boxes.filter(box => box.tokenIds.map(_.toString).contains(requiredTokenId))
      }
      .getOrElse(boxes)

    token.index
      .map { index =>
        val validBoxes: Seq[OnChainBox] = filteredById.filter(_.tokenIds.size > index)

        token.id.value
          .map { _ =>
            val requiredTokenId: String = token.id.getValue.toString
            validBoxes.filter(_.tokenIds(index).toString == requiredTokenId)
          }
          .getOrElse(validBoxes)
      }
      .getOrElse(filteredById)
  }
}
