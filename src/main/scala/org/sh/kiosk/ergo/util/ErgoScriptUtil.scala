package org.sh.kiosk.ergo.util

import java.security.SecureRandom

import org.sh.cryptonode.ecc.ECCPubKey
import org.sh.cryptonode.util.BytesUtil._
import org.sh.cryptonode.util.StringUtil._
import sigmastate.Values.ErgoTree
import sigmastate.basics.SecP256K1
import sigmastate.eval.SigmaDsl
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer
import sigmastate.serialization.ValueSerializer
import special.collection.Coll
import special.sigma.GroupElement

object ErgoScriptUtil {
  def hexToGroupElement(hex:String) = {
    val point = ECCPubKey(hex).point
    val secp256k1Point = SecP256K1.createPoint(point.x.bigInteger, point.y.bigInteger)
    SigmaDsl.GroupElement(secp256k1Point)
  }

  def groupElementToHex(groupElement: GroupElement) = {
    groupElement.getEncoded.toArray.encodeHex
  }

  def hexToErgoTree(hex:String) = {
    val bytes = hex.decodeHex
    DefaultSerializer.deserializeErgoTree(bytes)
  }

  def ergoTreeTohex(tree:ErgoTree) = {
    DefaultSerializer.serializeErgoTree(tree).encodeHex
  }

  def getConvertedValue(value:Any) = {
    value match {
      case bigInt:BigInt => SigmaDsl.BigInt(bigInt.bigInteger)
      case int:Int => int
      case long:Long => long
      case collBytes:Array[Byte] => sigmastate.eval.Colls.fromArray(collBytes)
      /*
    case collCollBytes:Array[Array[Byte]] =>
      val collArray = collCollBytes.map{collBytes =>
        sigmastate.eval.Colls.fromArray(collBytes)
      }
      sigmastate.eval.Colls.fromArray(collArray)
       */
      case grp:GroupElement => grp
      case any => ???
    }
  }

  def serialize(value:Any) = value match {
    case grp: GroupElement => grp.getEncoded.toArray
    case bigInt: special.sigma.BigInt => bigInt.toBytes.toArray
    case int: Int => ValueSerializer.serialize(int)
    case long: Long => ValueSerializer.serialize(long)
    case collByte: Coll[Byte] => collByte.toArray
    case any => ???
  }

  def getRandomBigInt = {
    val random = new SecureRandom()
    val values = new Array[Byte](32)
    random.nextBytes(values)
    BigInt(values).mod(SecP256K1.q)
  }


}
