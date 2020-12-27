package kiosk.offchain

import kiosk.encoding.ScalaErgoConverters
import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree => tree2addr, getStringFromAddress => addr2Str}
import kiosk.ergo.{KioskBox, KioskCollByte, KioskErgoTree, KioskLong, KioskType, StringToBetterString}
import kiosk.offchain.compiler.model.{Constant, DataType}

import java.util.UUID
import scala.util.Try

package object compiler {

  def tree2str(ergoTree: KioskErgoTree): String = addr2Str(tree2addr(ergoTree.value))

  case class CompileResult(dataInputBoxIds: Seq[String], inputBoxIds: Seq[String], inputNanoErgs: Long, inputTokens: Seq[(String, Long)], outputs: Seq[KioskBox], fee: Option[Long])

  case class DictionaryObject(isUnresolved: Boolean, declaration: Declaration)

  case class Variable(name: String, `type`: DataType.Type)

  def height(actualHeight: Int) = Constant("HEIGHT", DataType.Int, actualHeight.toString)

  def randId = UUID.randomUUID.toString

  case class OnChainBox(boxId: KioskCollByte, address: KioskErgoTree, nanoErgs: KioskLong, tokenIds: Seq[KioskCollByte], tokenAmounts: Seq[KioskLong], registers: Seq[KioskType[_]]) {
    lazy val stringTokenIds = tokenIds.map(_.toString)
    lazy val stringBoxId = boxId.toString
    require(tokenIds.size == tokenAmounts.size, s"tokenIds.size (${tokenIds.size}) != tokenAmounts.size (${tokenAmounts.size})")
  }

  case class Multiple[+This](seq: This*) {

    def exists(f: This => Boolean): Boolean = seq.exists(f)

    def take(i: Int): Multiple[This] = seq.take(i)

    def isEmpty = seq.isEmpty

    def map[That](thisToThat: This => That): Multiple[That] = seq.map(thisToThat)

    def foreach(thisToUnit: This => Unit): Unit = seq.foreach(thisToUnit)

    def forall(thisToBoolean: This => Boolean): Boolean = seq.forall(thisToBoolean)

    def filter(thisToBoolean: This => Boolean): Multiple[This] = seq.filter(thisToBoolean)

    def length: Int = seq.length

    def head = seq.head

    def zip[That](those: Multiple[That]): Multiple[(This, That)] = {
      (this.seq.length, those.seq.length) match {
        case (1, _)                                         => those.seq.map(this.seq.head -> _)
        case (_, 1)                                         => this.seq.map(_ -> those.seq.head)
        case (firstLen, secondLen) if firstLen == secondLen => this.seq zip those.seq
        case (firstLen, secondLen)                          => throw new Exception(s"Wrong number of elements in multi-pairs: first has $firstLen and second has $secondLen")
      }
    }

  }

  object Multiple {
    private implicit def seq2Multiple[T](seq: Seq[T]): Multiple[T] = Multiple(seq: _*)

    def sequence[T](seq: Seq[Multiple[T]]): Multiple[Seq[T]] = {
      seq.foldLeft(Multiple[Seq[T]](Nil)) {
        case (left, right) =>
          (left zip right).map {
            case (left, right) => left :+ right
          }
      }
    }
  }

  def noGapsInIndices(sorted: Seq[(Int, _)]): Boolean = sorted.map(_._1).zipWithIndex.forall { case (int, index) => int == index }

  def getMultiPairs(first: String, second: String)(implicit dictionary: Dictionary) = {
    Try(dictionary.getDeclaration(first).getValues zip dictionary.getDeclaration(second).getValues).fold(
      ex => throw new Exception(s"Error pairing $first and $second").initCause(ex),
      pairs => pairs
    )
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

  case class OnChain(name: String, var `type`: DataType.Type) extends Declaration {
    override lazy val maybeTargetId = Some(name)
    override lazy val pointerNames = Nil
    override lazy val pointerTypes = Nil
    override lazy val isLazy = true
    override def getValues(implicit dictionary: Dictionary) = dictionary.getOnChainValue(name)
    override lazy val canPointToOnChain: Boolean = false
  }

  def to[T](kioskTypes: Multiple[KioskType[_]]): Multiple[T] = kioskTypes.map(_.asInstanceOf[T])

  type T = (Option[_], String)

  def exactlyOne(obj: Any)(names: String*)(options: Option[_]*): Unit =
    if (options.count(_.isDefined) != 1) throw new Exception(s"Exactly one of {${names.toSeq.reduceLeft(_ + "," + _)}} must be defined in $obj")

  def atLeastOne(obj: Any)(names: String*)(options: Option[_]*): Unit =
    if (options.count(_.isDefined) == 0) throw new Exception(s"At least one of {${names.toSeq.reduceLeft(_ + "," + _)}} must be defined in $obj")

  def optSeq[T](s: Option[Seq[T]]) = s.toSeq.flatten

  def requireEmpty(data: T*) = {
    data.foreach {
      case (opt, message) => if (opt.isDefined) throw new Exception(s"$message cannot be defined: ${opt.get}")
    }
  }
  def requireDefined(data: T*) = {
    data.foreach {
      case (opt, message) => if (opt.isEmpty) throw new Exception(s"$message cannot be empty")
    }
  }
}
