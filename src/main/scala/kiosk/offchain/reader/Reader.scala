package kiosk.offchain.reader

import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo._
import kiosk.explorer.Explorer
import kiosk.offchain.compiler.model._
import kiosk.offchain.compiler.{Dictionary, OnChainBox, model, optSeq, randId}
import org.ergoplatform.ErgoAddress
import sigmastate.Values

class Reader(explorer: Explorer)(implicit dictionary: Dictionary) {
  def getBoxes(input: Input, alreadySelectedBoxIds: Seq[String]): Seq[OnChainBox] = {
    val boxById = for {
      id <- input.id
      _ <- id.value
    } yield {
      val boxId: String = id.getValue.toString
      OnChainBox.fromKioskBox(explorer.getBoxById(boxId))
    }

    val boxesByAddress = for {
      address <- input.address
      _ <- address.value
    } yield {
      val ergoTree: Values.ErgoTree = address.getValue.asInstanceOf[KioskErgoTree].value
      val ergoAddress: ErgoAddress = ScalaErgoConverters.getAddressFromErgoTree(ergoTree)
      val stringAddress: String = ScalaErgoConverters.getStringFromAddress(ergoAddress)
      explorer.getUnspentBoxes(stringAddress).map(OnChainBox.fromKioskBox)
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

  private case class TokenBox(onChainBox: OnChainBox, ids: Set[ID])

  private def filterByTokens(boxes: Seq[OnChainBox], tokens: Seq[model.Token], strict: Boolean): Seq[OnChainBox] =
    tokens.foldLeft(boxes.map(box => TokenBox(box, Set.empty[String])))((before, token) => filterByToken(before, token)).collect {
      case TokenBox(box, ids) if !strict || box.stringTokenIds.toSet == ids => box
    }

  private def matches(tokenAmount: KioskLong, long: model.Long): Boolean = long.getFilterTarget.map(FilterOp.matches(tokenAmount, _, long.filterOp)).getOrElse(true)

  private def dummyId = Id(name = Some(randId), value = None)
  private def dummyLong = Long(name = Some(randId), value = None, filter = None)

  private def filterByToken(boxes: Seq[TokenBox], token: model.Token): Seq[TokenBox] = {
    val tokenId: Id = token.id.getOrElse(dummyId)
    val amount: Long = token.amount.getOrElse(dummyLong)
    (token.index, tokenId.value) match {
      case (Some(index), Some(_)) =>
        val id: String = tokenId.getValue.toString
        boxes
          .filter(box => box.onChainBox.tokenIds(index).toString == id && matches(box.onChainBox.tokenAmounts(index), amount))
          .map(box => box.copy(ids = box.ids + id))
      case (Some(index), None) =>
        boxes
          .filter(box => box.onChainBox.tokenIds.size > index && matches(box.onChainBox.tokenAmounts(index), amount))
          .map(box => box.copy(ids = box.ids + box.onChainBox.tokenIds(index).toString))
      case (None, Some(_)) =>
        val id: String = tokenId.getValue.toString
        boxes
          .filter(box => box.onChainBox.stringTokenIds.contains(id) && matches(box.onChainBox.tokenAmounts(box.onChainBox.stringTokenIds.indexOf(id)), amount))
          .map(box => box.copy(ids = box.ids + id))
      case _ => throw new Exception(s"At least one of token.index or token.id.value must be defined in $token")
    }
  }
}
