package kiosk

import org.bouncycastle.util.encoders.Hex
import org.ergoplatform.appkit.{ErgoType, ErgoValue}
import sigmastate.SGroupElement
import sigmastate.Values.{ByteArrayConstant, CollectionConstant, ErgoTree}
import sigmastate.basics.SecP256K1
import sigmastate.eval.SigmaDsl
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer
import sigmastate.serialization.ValueSerializer
import special.collection.Coll
import special.sigma
import special.sigma.GroupElement

import scala.io.BufferedSource
import scala.util.Try

package object ergo {
  class BetterString(string: String) {
    def decodeHex = Hex.decode(string)
  }

  implicit def ByteArrayToBetterByteArray(bytes: Array[Byte]) = new BetterByteArray(bytes)

  class BetterByteArray(bytes: Seq[Byte]) {
    def encodeHex: String = Hex.toHexString(bytes.toArray).toLowerCase
  }

  implicit def StringToBetterString(string: String) = new BetterString(string)

  sealed trait KioskType[T] {
    val serialize: Array[Byte]
    val value: T
    lazy val hex = serialize.encodeHex
    def getErgoValue: ErgoValue[_]

    val typeName: String
    override def toString = value.toString
  }

  case class KioskCollByte(arrayBytes: Array[Byte]) extends KioskType[Coll[Byte]] {
    override val value: Coll[Byte] = sigmastate.eval.Colls.fromArray(arrayBytes)
    override val serialize: Array[Byte] = ValueSerializer.serialize(ByteArrayConstant(value))
    override def toString: String = arrayBytes.encodeHex
    override val typeName: String = "Coll[Byte]"
    override def getErgoValue = ErgoValue.of(arrayBytes)
  }

  case class KioskCollGroupElement(groupElements: Array[GroupElement]) extends KioskType[Coll[GroupElement]] {
    override val value: Coll[GroupElement] = sigmastate.eval.Colls.fromArray(groupElements)
    override val serialize: Array[Byte] = ValueSerializer.serialize(CollectionConstant[SGroupElement.type](value, SGroupElement))
    override def toString: String = "[" + groupElements.map(_.getEncoded.toArray.encodeHex).reduceLeft(_ + "," + _) + "]"
    override val typeName: String = "Coll[GroupElement]"
    override def getErgoValue = ErgoValue.of(groupElements, ErgoType.groupElementType)
  }

  case class KioskInt(value: Int) extends KioskType[Int] {
    override val serialize: Array[Byte] = ValueSerializer.serialize(value)
    override val typeName: String = "Int"
    override def getErgoValue = ErgoValue.of(value)
  }

  case class KioskLong(value: Long) extends KioskType[Long] {
    override val serialize: Array[Byte] = ValueSerializer.serialize(value)
    override val typeName: String = "Long"
    override def getErgoValue = ErgoValue.of(value)
  }

  case class KioskBigInt(bigInt: BigInt) extends KioskType[sigma.BigInt] {
    override val value: sigma.BigInt = SigmaDsl.BigInt(bigInt.bigInteger)
    override val serialize: Array[Byte] = ValueSerializer.serialize(value)
    override val typeName: String = "BigInt"
    override def toString: String = bigInt.toString(10)
    override def getErgoValue = ErgoValue.of(bigInt.bigInteger)
  }

  case class KioskGroupElement(value: GroupElement) extends KioskType[GroupElement] {
    override val serialize: Array[Byte] = ValueSerializer.serialize(value)
    override def toString: String = value.getEncoded.toArray.encodeHex
    override val typeName: String = "GroupElement"
    override def getErgoValue = ErgoValue.of(value)
    def +(that: KioskGroupElement) = KioskGroupElement(value.multiply(that.value))
  }

  lazy val PointAtInfinity = KioskGroupElement(SigmaDsl.GroupElement(SecP256K1.identity))

  case class KioskErgoTree(value: ErgoTree) extends KioskType[ErgoTree] {
    override val serialize: Array[Byte] = DefaultSerializer.serializeErgoTree(value)
    override val typeName: String = "ErgoTree"

    override def getErgoValue = ??? // should never be needed
    override def toString: ID = "<ergo tree>"
  }

  implicit def groupElementToKioskGroupElement(g: GroupElement) = KioskGroupElement(g)

  case class DhtData(g: GroupElement, h: GroupElement, u: GroupElement, v: GroupElement, x: BigInt)

  type ID = String
  type Amount = Long

  type Token = (ID, Amount)
  type Tokens = Array[Token]

  def decodeBigInt(encoded: String): BigInt = Try(BigInt(encoded, 10)).recover { case ex => BigInt(encoded, 16) }.get

  case class KioskBox(
      address: String,
      value: Long,
      registers: Array[KioskType[_]],
      tokens: Tokens,
      optBoxId: Option[String] = None,
      spentTxId: Option[String] = None
  )

  def usingSource[B](param: BufferedSource)(f: BufferedSource => B): B =
    try f(param)
    finally param.close

  abstract class MyEnum extends Enumeration {
    def fromString(str: String): Value =
      values
        .find(value => value.toString.equalsIgnoreCase(str))
        .getOrElse(throw new Exception(s"Invalid op $str. Permitted options are ${values.map(_.toString).reduceLeft(_ + ", " + _)}"))
    def toString(op: Value): String = op.toString
  }
}
