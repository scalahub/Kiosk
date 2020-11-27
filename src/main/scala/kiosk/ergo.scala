package kiosk

import org.apache.commons.codec.binary.Hex
import org.ergoplatform.appkit.{ErgoType, ErgoValue}
import org.json.JSONObject
import org.sh.utils.json.JSONUtil.JsonFormatted
import sigmastate.SGroupElement
import sigmastate.Values.{ByteArrayConstant, CollectionConstant, ErgoTree}
import sigmastate.eval.SigmaDsl
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer
import sigmastate.serialization.ValueSerializer
import special.collection.Coll
import special.sigma
import special.sigma.GroupElement

package object ergo {
  class BetterString(string: String) {
    def decodeHex = Hex.decodeHex(string)
  }

  implicit def ByteArrayToBetterByteArray(bytes: Array[Byte]) = new BetterByteArray(bytes)

  class BetterByteArray(bytes: Seq[Byte]) {
    def encodeHex: String = Hex.encodeHexString(bytes.toArray).toLowerCase
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
  }

  case class KioskErgoTree(value: ErgoTree) extends KioskType[ErgoTree] {
    override val serialize: Array[Byte] = DefaultSerializer.serializeErgoTree(value)
    override val typeName: String = "ErgoTree"
    override def getErgoValue = ??? // should never be needed
    override def toString: ID = "<ergo tree>"
  }

  implicit def groupElementToKioskGroupElement(g: GroupElement) = KioskGroupElement(g)

  case class DhtData(g: GroupElement, h: GroupElement, u: GroupElement, v: GroupElement, x: BigInt) extends JsonFormatted {
    val keys = Array("g", "h", "u", "v")
    val vals = Array(g.hex, h.hex, u.hex, v.hex)
  }

  type ID = String
  type Amount = Long

  type Token = (ID, Amount)
  type Tokens = Array[Token]

  def regs2Json(registers: Array[KioskType[_]]) = {
    var ctr = 4
    registers.map { register =>
      val jo = new JSONObject()
      val name = s"R$ctr"
      jo.put(name, register.serialize.encodeHex)
      jo.put("type", register.typeName)
      ctr += 1
      jo
    }
  }

  def tokens2Json(tokens: Tokens) = {
    var ctr = 0
    tokens.map { token =>
      val jo = new JSONObject()
      val (id, amount) = token
      jo.put("index", ctr)
      jo.put("id", id)
      jo.put("amount", amount)
      ctr += 1
      jo
    }
  }

  case class KioskBox(address: String, value: Long, registers: Array[KioskType[_]], tokens: Tokens, optBoxId: Option[String] = None, spentTxId: Option[String] = None) extends JsonFormatted {
    val keys = Array[String]("address", "value", "registers", "tokens", "boxId", "spentTxId")
    val vals = Array[Any](address, value, regs2Json(registers), tokens2Json(tokens), optBoxId.getOrElse("none"), spentTxId.getOrElse("none"))
  }

}
