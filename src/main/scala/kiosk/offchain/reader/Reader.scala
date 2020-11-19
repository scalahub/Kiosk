package kiosk.offchain.reader

import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo._
import kiosk.explorer.Explorer
import kiosk.offchain.compiler.{Dictionary, OnChainBox, optSeq}
import kiosk.offchain.model
import kiosk.offchain.model.FilterOp.Op
import kiosk.offchain.model.{DataType, FilterOp, Input, RegNum, Register}
import sigmastate.Values

object Reader {
  def getBox(input: Input)(dictionary: Dictionary): Option[OnChainBox] = {
    val boxById = input.boxId.flatMap { id =>
      id.value.map { name =>
        OnChainBox.fromKioskBox(Explorer.getBoxById(dictionary.getValue(name).asInstanceOf[KioskCollByte].value.toArray.encodeHex))
      }
    }.toSeq

    val boxesByAddress = optSeq(
      input.address.flatMap { address =>
        address.value.map { name =>
          val ergoTree: Values.ErgoTree = dictionary.getValue(name).asInstanceOf[KioskErgoTree].value
          val address: String = ScalaErgoConverters.getStringFromAddress(ScalaErgoConverters.getAddressFromErgoTree(ergoTree))
          Explorer.getUnspentBoxes(address).map(OnChainBox.fromKioskBox)
        }
      }
    )

    val matchedBoxes = boxById ++ boxesByAddress

    val filteredByRegisters = filterByRegisters(matchedBoxes, optSeq(input.registers))(dictionary)

    val filteredByTokens = filterByTokens(filteredByRegisters, optSeq(input.tokens))(dictionary)

    val filteredByNanoErgs: Seq[OnChainBox] = input.nanoErgs
      .flatMap { nanoErgs =>
        nanoErgs.value.map { ref =>
          val required: KioskLong = getLong(ref)(dictionary)
          val filter: Op = nanoErgs.filter.getOrElse(FilterOp.Eq)
          filteredByTokens.filter { onChainBox =>
            FilterOp.matches(onChainBox.nanoErgs, required, filter)
          }
        }
      }
      .getOrElse(filteredByTokens)

    filteredByNanoErgs.headOption
  }

  private def filterByRegisters(boxes: Seq[OnChainBox], registers: Seq[Register])(dictionary: Dictionary): Seq[OnChainBox] = {
    registers.foldLeft(boxes)((boxesBeforeFilter, register) => filterByRegister(boxesBeforeFilter, register)(dictionary))
  }

  private def filterByRegister(boxes: Seq[OnChainBox], register: Register)(dictionary: Dictionary): Seq[OnChainBox] = {
    val index = RegNum.getIndex(register.num)
    val filteredByType = boxes.filter(box => DataType.isValid(box.registers(index), register.`type`))
    register.value
      .map { ref =>
        val expected: KioskType[_] = dictionary.getValue(ref)
        filteredByType.filter { box =>
          val kioskType: KioskType[_] = box.registers(index)
          kioskType.typeName == expected.typeName && kioskType.hex == expected.hex
        }
      }
      .getOrElse(filteredByType)
  }

  private def filterByTokens(boxes: Seq[OnChainBox], tokens: Seq[model.Token])(dictionary: Dictionary): Seq[OnChainBox] = {
    tokens.foldLeft(boxes)((boxesBeforeFilter, token) => filterByToken(boxesBeforeFilter, token)(dictionary))
  }

  private def filterByToken(onChainBoxes: Seq[OnChainBox], token: model.Token)(dictionary: Dictionary): Seq[OnChainBox] = {
    val index = token.index

    val filteredByTokenId = token.tokenId.value
      .map { ref =>
        val requiredTokenId: KioskType[_] = dictionary.getValue(ref)
        onChainBoxes.filter { onChainBox =>
          val actualTokenId: KioskCollByte = onChainBox.tokenIds(index)
          actualTokenId.typeName == requiredTokenId.typeName && actualTokenId.hex == requiredTokenId.hex
        }
      }
      .getOrElse(onChainBoxes)

    token.amount.value
      .map {
        case ref =>
          val required: KioskLong = getLong(ref)(dictionary)
          val filter: Op = token.amount.filter.getOrElse(FilterOp.Eq)
          filteredByTokenId.filter { onChainBox =>
            FilterOp.matches(onChainBox.tokenAmounts(index), required, filter)
          }
      }
      .getOrElse(filteredByTokenId)
  }

  private def getLong(ref: String)(dictionary: Dictionary) = {
    dictionary.getValue(ref) match {
      case kioskLong: KioskLong => kioskLong
      case any                  => throw new Exception(s"Required KioskLong. Found ${any.typeName}")
    }
  }
}
