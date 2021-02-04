package kiosk

import org.bouncycastle.util.encoders.Hex
import org.ergoplatform.appkit.{ErgoType, ErgoValue}
import play.api.libs.json.{JsString, JsValue, Json, Writes}
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

  private implicit val writesGroupElement = new Writes[GroupElement] {
    override def writes(o: GroupElement): JsValue = new JsString(o.hex)
  }
  private implicit val writesDhtData = Json.writes[DhtData]

  case class DhtData(g: GroupElement, h: GroupElement, u: GroupElement, v: GroupElement, x: BigInt) {
    override def toString = Json.toJson(this).toString()
  }

  type ID = String
  type Amount = Long

  type Token = (ID, Amount)
  type Tokens = Array[Token]

  def decodeBigInt(encoded: String): BigInt = Try(BigInt(encoded, 10)).recover { case ex => BigInt(encoded, 16) }.get

  private implicit val writesKioskType = new Writes[KioskType[_]] {
    override def writes(o: KioskType[_]): JsValue = JsString(o.toString)
  }
  private implicit val writesKioskBox = Json.writes[KioskBox]

  case class KioskBox(address: String, value: Long, registers: Array[KioskType[_]], tokens: Tokens, optBoxId: Option[String] = None, spentTxId: Option[String] = None) {
    override def toString = Json.toJson(this).toString()
  }

  def usingSource[B](param: BufferedSource)(f: BufferedSource => B): B =
    try f(param)
    finally param.close

}
