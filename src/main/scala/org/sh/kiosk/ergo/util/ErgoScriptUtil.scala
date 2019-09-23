package org.sh.kiosk.ergo.util

import java.security.SecureRandom

import org.json.JSONObject
import org.sh.cryptonode.ecc.ECCPubKey
import org.sh.cryptonode.util.BytesUtil._
import org.sh.cryptonode.util.StringUtil._
import org.sh.utils.json.JSONUtil.JsonFormatted
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

  def ergoTreeToHex(tree:ErgoTree) = {
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
    case grp: GroupElement => grp.getEncoded.toArray // ValueSerializer.serialize(grp)
    case bigInt: special.sigma.BigInt => bigInt.toBytes.toArray // ValueSerializer.serialize(bigInt)
    case int: Int => ValueSerializer.serialize(int)
    case long: Long => ValueSerializer.serialize(long)
    case collByte: Coll[Byte] => collByte.toArray
    case ergoTree: ErgoTree => DefaultSerializer.serializeErgoTree(ergoTree)
    case any => ???
  }

  def getRandomBigInt = {
    val random = new SecureRandom()
    val values = new Array[Byte](32)
    random.nextBytes(values)
    BigInt(values).mod(SecP256K1.q)
  }


  type Register = Array[Byte]
  type Registers = Array[Register]
  type ID = Array[Byte]
  type Amount = Long

  type Token = (ID, Amount)
  type Tokens = Array[Token]

  def $fromRegs(registers: Registers) = {
    var ctr = 4
    registers.map{register =>
      val jo = new JSONObject()
      val name = s"R$ctr"
      jo.put(name, register.encodeHex)
      ctr += 1
      jo
    }
  }

  def $fromTokens(tokens: Tokens) = {
    var ctr = 0
    tokens.map{token =>
      val jo = new JSONObject()
      val (id, amount) = token
      jo.put("index", ctr)
      jo.put("id", id)
      jo.put("amount", amount)
      ctr += 1
      jo
    }
  }

  case class Box(ergoTree: ErgoTree, registers: Registers, tokens: Tokens) extends JsonFormatted {
    val keys = Array[String]("ergoTree", "registers", "tokens")
    val vals = Array[Any](serialize(ergoTree).encodeHex, $fromRegs(registers), $fromTokens(tokens))
  }


}
