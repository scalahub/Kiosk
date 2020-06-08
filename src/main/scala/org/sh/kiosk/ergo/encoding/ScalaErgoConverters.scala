package org.sh.kiosk.ergo.encoding

import java.math.BigInteger

import org.bouncycastle.math.ec.custom.sec.SecP256K1Point
import org.ergoplatform.ErgoAddress
import org.sh.cryptonode.ecc.ECCPubKey
import org.sh.cryptonode.util.StringUtil._
import org.sh.kiosk.ergo._
import org.sh.kiosk.ergo.script.ErgoScript
import sigmastate.Values.{ConstantNode, ErgoTree}
import sigmastate._
import sigmastate.basics.SecP256K1
import sigmastate.eval.SigmaDsl
import sigmastate.eval.bigIntToBigInteger
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer
import sigmastate.serialization.ValueSerializer
import special.collection.Coll
import special.sigma.{BigInt, GroupElement}

object ScalaErgoConverters {

  def stringToGroupElement(hex:String): GroupElement = {
    val point = ECCPubKey(hex).point
    val secp256k1Point: SecP256K1Point = SecP256K1.createPoint(point.x.bigInteger, point.y.bigInteger)
    SigmaDsl.GroupElement(secp256k1Point)
  }

  def groupElementToString(groupElement: GroupElement): String = KioskGroupElement(groupElement).toString

  def stringToErgoTree(hex:String): ErgoTree = DefaultSerializer.deserializeErgoTree(hex.decodeHex)

  def ergoTreeToString(tree:ErgoTree): String = KioskErgoTree(tree).hex

  def getAddressFromErgoTree(ergoTree: ErgoTree) = ErgoScript.$ergoAddressEncoder.fromProposition(ergoTree).get

  def getStringFromAddress(ergoAddress: ErgoAddress):String = ErgoScript.$ergoAddressEncoder.toString(ergoAddress)

  def getAddressFromString(string: String) = ErgoScript.$ergoAddressEncoder.fromString(string).get

  def deserialize(hex:String):KioskType[_] = {
    val bytes = hex.decodeHex
    val value: Values.Value[SType] = ValueSerializer.deserialize(bytes)

    value match {
      case ConstantNode(g, SGroupElement) => KioskGroupElement(g.asInstanceOf[GroupElement])
      case ConstantNode(i, SBigInt) =>
        val bigInteger:BigInteger = i.asInstanceOf[BigInt]
        KioskBigInt(scala.BigInt(bigInteger))
      case ConstantNode(l, SLong) => KioskLong(l.asInstanceOf[Long])
      case ConstantNode(c, _:SCollection[_]) => KioskCollByte(c.asInstanceOf[Coll[Byte]].toArray)
      case ConstantNode(i, SInt) => KioskInt(i.asInstanceOf[Int])
      case any => throw new Exception(s"Unsupported encoded data $hex (decoded as $any)")
    }
  }
}
