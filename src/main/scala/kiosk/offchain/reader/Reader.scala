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

    val filteredByTokens = filterByTokens(filteredByRegisters, optSeq(input.tokens), input.strict)

    val filteredByNanoErgs = for {
      nanoErgs <- input.nanoErgs
      target <- nanoErgs.getFilterTarget
    } yield filteredByTokens.filter(onChainBox => FilterOp.matches(onChainBox.nanoErgs, target, nanoErgs.filterOp))

    filteredByNanoErgs.getOrElse(filteredByTokens)
  }

  private def filterByRegisters(boxes: Seq[OnChainBox], registers: Seq[Register]): Seq[OnChainBox] =
    registers.foldLeft(boxes)((boxesBeforeFilter, register) => filterByRegister(boxesBeforeFilter, register))

  private def filterByRegister(boxes: Seq[OnChainBox], register: Register): Seq[OnChainBox] = {
    val index: Int = RegNum.getIndex(register.num)
    val filteredByType: Seq[OnChainBox] = boxes.filter(box => DataType.isValid(box.registers(index), register.`type`))

    register.value match {
      case Some(_) =>
        val expected: KioskType[_] = register.getValue
        filteredByType.filter { box =>
          val kioskType: KioskType[_] = box.registers(index)
          kioskType.typeName == expected.typeName && kioskType.hex == expected.hex
        }
      case _ => filteredByType
    }
  }

  private def filterByTokens(boxes: Seq[OnChainBox], tokens: Seq[model.Token], strict: Boolean): Seq[OnChainBox] =
    tokens.foldLeft(boxes.map(box => box -> Set.empty[String]))((before, token) => filterByToken(before, token)).collect {
      case (box, matchedIds) if !strict || box.stringTokenIds.toSet == matchedIds => box
    }

  private def filterByToken(boxes: Seq[(OnChainBox, Set[ID])], token: model.Token): Seq[(OnChainBox, Set[ID])] = (token.index, token.id.value) match {
    case (Some(index), Some(_)) =>
      val id: String = token.id.getValue.toString
      boxes.filter(_._1.tokenIds(index).toString == id).map { case (box, matchedIds) => box -> (matchedIds + id) }
    case (Some(index), None) =>
      boxes.filter(_._1.tokenIds.size > index).map { case (box, matchedId) => box -> (matchedId + box.tokenIds(index).toString) }
    case (None, Some(_)) =>
      val id: String = token.id.getValue.toString
      boxes.filter(_._1.stringTokenIds.contains(id)).map { case (box, matchedIds) => box -> (matchedIds + id) }
    case _ => ??? // should never happen
  }
}
