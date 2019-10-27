package org.sh.kiosk.ergo

import org.sh.kiosk.ergo.util.ErgoScriptUtil.{getConvertedValue, serialize}
import org.sh.reflect.DefaultTypeHandler
import special.sigma.GroupElement
import org.sh.cryptonode.util.BytesUtil._
import scorex.crypto.hash.Blake2b256
import sigmastate.basics.SecP256K1
import org.sh.cryptonode.ecc.{ECCPubKey, Point}
import org.sh.kiosk.ergo.util.ErgoScriptUtil._

class Env {

  private [ergo] var $scala_env:Map[String, Any] = Map()

  def getAll = {
    $scala_env.toArray.map{
      case (name, value) =>
        val (elValue, elType) = value match {
          case grp: GroupElement => (grp.getEncoded.toArray.encodeHex, "GroupElement")
          case bigInt: BigInt => (bigInt.toString(10), "BigInt")
          case arrayByte: Array[Byte] => (DefaultTypeHandler.typeToString(arrayByte.getClass, arrayByte), "Coll[Byte]")
          case arrayArrayByte: Array[Array[Byte]] => (DefaultTypeHandler.typeToString(arrayArrayByte.getClass, arrayArrayByte), "Coll[Coll[Byte]]")
          case any => (any.toString, any.getClass)
        }
        s"""{"name":"$name", "value":"${elValue}", "type":"${elType}"}"""
    }
  }

  def $getEnv = {
    // use def because $scala_env can be modified anytime and we need to use the latest one
    $scala_env.map{ case (key, value) => key -> getConvertedValue(value) }
  }

  def setGroupElement(name:String, groupElement: GroupElement) = {
    val $INFO$ = "A group element is encoded as a public key of Bitcoin in hex (compressed or uncompressed)"
    val $name$ = "g"
    val $groupElement$ = "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"
    $scala_env += name -> groupElement
    groupElement
  }

  def deleteAll = {
    $scala_env = Map()
  }

  def delete(key:String) = {
    val $key$ = "b"
    $scala_env -= key
  }

  def setBigInt(name:String, bigInt:BigInt) = {
    val $name$ = "b"
    val $bigInt$ = "1234567890123456789012345678901234567890"
    $scala_env += name -> bigInt
  }
  def setLong(name:String, long:Long) = {
    val $name$ = "long"
    val $long$ = "12345678901112"
    $scala_env += name -> long
  }
  def setInt(name:String, int:Int) = {
    val $name$ = "int"
    val $int$ = "123456789"
    $scala_env += name -> int
  }
  def setCollByte(name:String, collBytes:Array[Byte]) = {
    val $name$ = "c"
    val $collBytes$ = "0x1a2b3c4d5e6f"
    $scala_env += name -> collBytes
  }

  @deprecated("Not fully supported")
  def $setCollCollByte(name:String, collCollBytes:Array[Array[Byte]]) = {
    val $name$ = "d"
    val $collCollBytes$ = "[0x1a2b3c4d5e6f,0xafbecddc12,0xa132]"
    $scala_env += name -> collCollBytes
  }


  def $getKeysFromEnv(keys:Array[String]) = {
    keys.map{key =>
      val value = $getEnv.get(key).getOrElse(throw new Exception(s"Environment does not contain key $key"))
      key -> value
    }
  }
  def $regex(scriptBytes: Array[Byte], keysToMatch:Array[String]) = {
    val hex = scriptBytes.encodeHex
    val startRegex = s"^$hex$$"
    $getKeysFromEnv(keysToMatch).foldLeft(startRegex)(
      (currRegex, y) => {
        val (keyword, value) = y
        val serialized:Array[Byte] = serialize(value)
        val encodedValue = serialized.encodeHex
        val replacement = s"[0-9a-fA-F]{${encodedValue.size}}"
        currRegex.replace(encodedValue, replacement)
      }
    )
  }

  def $matchScript(scriptBytes: Array[Byte], keysToMatch:Array[String]):String = {
    $getKeysFromEnv(keysToMatch).foldLeft(scriptBytes.encodeHex)(
      (currStr, y) => {
        val (keyword, value) = y

        val serialized:Array[Byte] = serialize(value)
        val encodedValue = serialized.encodeHex
        val value_r = encodedValue.length / 2
        val value_l = encodedValue.length - value_r
        val kw_r = keyword.length / 2
        val kw_l = keyword.length - kw_r
        val replacement = "<" + ("-" * (value_r - kw_r - 1)) + keyword + ("-" * (value_l - kw_l-1)) + ">"
        currStr.replace(encodedValue, replacement)
      }
    )
  }
}
