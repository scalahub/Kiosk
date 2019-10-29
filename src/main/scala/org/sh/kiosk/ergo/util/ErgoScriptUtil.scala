package org.sh.kiosk.ergo.util

import java.security.SecureRandom

import org.json.JSONObject
import org.sh.cryptonode.ecc.ECCPubKey
import org.sh.cryptonode.util.BytesUtil._
import org.sh.cryptonode.util.StringUtil._
import org.sh.reflect.DefaultTypeHandler
import org.sh.utils.json.JSONUtil.JsonFormatted
import sigmastate.Values.{ByteArrayConstant, ErgoTree, Value}
import sigmastate.basics.SecP256K1
import sigmastate.eval.SigmaDsl
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer
import sigmastate.serialization.{DataSerializer, OpCodes, SigmaSerializer, ValueSerializer}
import sigmastate.utils.SigmaByteWriter
import special.collection.Coll
import special.sigma.GroupElement

object ErgoScriptUtil {
  DefaultTypeHandler.addType[GroupElement](classOf[GroupElement], hexToGroupElement, groupElementToHex)
  DefaultTypeHandler.addType[ErgoTree](classOf[ErgoTree], hexToErgoTree, ergoTreeToHex)


  @deprecated("Unused as of now", "27 Aug 2019")
  def arrArrByteToCollCollByte(a:Array[Array[Byte]]) = {
    val collArray = a.map{colByte =>
      sigmastate.eval.Colls.fromArray(colByte)
    }
    sigmastate.eval.Colls.fromArray(collArray)
  }

  def arrByteToCollByte(a:Array[Byte]) = sigmastate.eval.Colls.fromArray(a)
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
    case grp: GroupElement => ValueSerializer.serialize(grp) //grp.getEncoded.toArray // ValueSerializer.serialize(grp)
    case bigInt: special.sigma.BigInt => ValueSerializer.serialize(bigInt) // bigInt.toBytes.toArray // ValueSerializer.serialize(bigInt)
    case int: Int => ValueSerializer.serialize(int)
    case long: Long => ValueSerializer.serialize(long)
    case collByte: Coll[Byte] =>
      //Array[Byte](0x0e,collByte.size.toByte)++collByte.toArray
      sigmastate.serialization.ValueSerializer.serialize(ByteArrayConstant(collByte))
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

  case class Box(address:String, value:Long, registers: Registers, tokens: Tokens) extends JsonFormatted {
    val keys = Array[String]("address", "value", "registers", "tokens")
    val vals = Array[Any](address, value, $fromRegs(registers), $fromTokens(tokens))
  }


}