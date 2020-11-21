package kiosk.offchain

import java.util.UUID

import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo
import kiosk.ergo.{KioskBox, KioskCollByte, KioskErgoTree, KioskLong, KioskType, StringToBetterString}
import kiosk.offchain.model.{Constant, DataType}

package object compiler {
  case class CompileResult(dataInputBoxIds: Seq[String], inputBoxIds: Seq[String], inputNanoErgs: Long, inputTokens: Seq[(String, Long)], outputs: Seq[KioskBox], fee: Option[Long])

  case class DictionaryObject(isUnresolved: Boolean, declaration: Declaration)

  case class Variable(name: String, `type`: DataType.Type)

  def height(actualHeight: Int) = Constant("HEIGHT", DataType.Int, actualHeight.toString)

  def randId = UUID.randomUUID.toString

  case class OnChainBox(boxId: KioskCollByte, address: KioskErgoTree, nanoErgs: KioskLong, tokenIds: Seq[KioskCollByte], tokenAmounts: Seq[KioskLong], registers: Seq[KioskType[_]]) {
    require(tokenIds.size == tokenAmounts.size, s"tokenIds.size (${tokenIds.size}) != tokenAmounts.size (${tokenAmounts.size})")
  }

  object OnChainBox {
    def fromKioskBox(kioskBox: KioskBox) = {
      kioskBox.spentTxId.map(_ => throw new Exception(s"Box id ${kioskBox.optBoxId.get} has been spent"))
      val address = KioskErgoTree(ScalaErgoConverters.getAddressFromString(kioskBox.address).script)
      val nanoErgs = KioskLong(kioskBox.value)
      val boxIdHex = kioskBox.optBoxId.getOrElse(throw new Exception(s"No box id found in $kioskBox"))
      val boxId = KioskCollByte(boxIdHex.decodeHex)
      val registers = kioskBox.registers.toSeq
      val (tokenIdsHex, tokenValues) = kioskBox.tokens.unzip
      val tokenIds = tokenIdsHex.map(tokenIdHex => KioskCollByte(tokenIdHex.decodeHex)).toSeq
      val tokenAmounts = tokenValues.map(tokenValue => KioskLong(tokenValue)).toSeq
      OnChainBox(boxId, address, nanoErgs, tokenIds, tokenAmounts, registers)
    }
  }

  case class OnChainConstant(name: String, var `type`: DataType.Type) extends Declaration {
    override lazy val maybeId = Some(name)
    override lazy val refs = Nil
    override lazy val refTypes = Nil
    override lazy val isLazy = true
    override def getValue(dictionary: Dictionary): ergo.KioskType[_] = dictionary.getOnChainValue(name)
    override lazy val possiblyOnChain: Boolean = false
  }

  def exactlyOne(obj: Any)(names: String*)(options: Option[_]*): Unit =
    if (options.count(_.isDefined) != 1) throw new Exception(s"Exactly one of {${names.toSeq.reduceLeft(_ + "," + _)}} must be defined in $obj")

  def atLeastOne(obj: Any)(names: String*)(options: Option[_]*): Unit =
    if (options.count(_.isDefined) == 0) throw new Exception(s"At least one of {${names.toSeq.reduceLeft(_ + "," + _)}} must be defined in $obj")

  def optSeq[T](s: Option[Seq[T]]) = s.toSeq.flatten
}
