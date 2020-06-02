package org.sh.kiosk.ergo.encoding

import org.bouncycastle.math.ec.custom.sec.SecP256K1Point
import org.sh.cryptonode.ecc.ECCPubKey
import org.sh.cryptonode.util.BytesUtil._
import org.sh.cryptonode.util.StringUtil._
import sigmastate.Values.{ByteArrayConstant, ErgoTree}
import sigmastate.basics.SecP256K1
import sigmastate.eval.SigmaDsl
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer
import sigmastate.serialization.ValueSerializer
import special.collection.Coll
import special.sigma.GroupElement

object ScalaErgoConverters {

  def arrByteToCollByte(a:Array[Byte]): Coll[Byte] = sigmastate.eval.Colls.fromArray(a)

  def hexToGroupElement(hex:String): GroupElement = {
    val point = ECCPubKey(hex).point
    val secp256k1Point: SecP256K1Point = SecP256K1.createPoint(point.x.bigInteger, point.y.bigInteger)
    SigmaDsl.GroupElement(secp256k1Point)
  }

  def groupElementToHex(groupElement: GroupElement): String = {
    groupElement.getEncoded.toArray.encodeHex
  }

  def hexToErgoTree(hex:String): ErgoTree = {
    val bytes = hex.decodeHex
    DefaultSerializer.deserializeErgoTree(bytes)
  }

  def ergoTreeToHex(tree:ErgoTree): String = {
    DefaultSerializer.serializeErgoTree(tree).encodeHex
  }

  // converts from Scala's native types (such as Array[Byte], BigInt) to SigmaDsl types (such as SigmaDsl.BigInt, Coll[Byte])
  // Scala's types are used for getting input from end-user and then this method is used to convert them to types that we will use
  def getConvertedValue(value:Any) = {
    value match {
      case bigInt:BigInt => SigmaDsl.BigInt(bigInt.bigInteger)
      case int:Int => int
      case long:Long => long
      case arrayBytes:Array[Byte] => sigmastate.eval.Colls.fromArray(arrayBytes)
      case arrayArrayBytes:Array[Array[Byte]] =>
        val collArray = arrayArrayBytes.map{arrayBytes =>
          sigmastate.eval.Colls.fromArray(arrayBytes)
        }
        sigmastate.eval.Colls.fromArray(collArray)
      case grp:GroupElement => grp
      case any => ???
    }
  }

  /*
    Serializes SigmaDsl data types. Note that it only accepts SigmaDsl types (such as Coll[Byte], etc) and Scala's native types (such as Array[Byte])
     must be converted to SigmaDsl types first (using getConvertedValue method) before being passed to this method.
   */
  def serialize(value:Any) = value match {
    case grp: GroupElement => ValueSerializer.serialize(grp) //grp.getEncoded.toArray // ValueSerializer.serialize(grp)
    case bigInt: special.sigma.BigInt => ValueSerializer.serialize(bigInt) // bigInt.toBytes.toArray // ValueSerializer.serialize(bigInt)
    case int: Int => ValueSerializer.serialize(int)
    case long: Long => ValueSerializer.serialize(long)
    case ergoTree: ErgoTree => DefaultSerializer.serializeErgoTree(ergoTree)
    case collByte: Coll[Byte] =>
      sigmastate.serialization.ValueSerializer.serialize(ByteArrayConstant(collByte))
    case any => ???
  }
}
