package org.sh.kiosk.ergo.script

import org.sh.kiosk.ergo.encoding.{EasyWebEncoder, ScalaErgoConverters}
import special.sigma.GroupElement


class ErgoScriptEnv {

  // Any variable starting with '$' is hidden from the auto-generated frontend of EasyWeb
  var $scala_env:Map[String, Any] = Map()

  def setGroupElement(name:String, groupElement: GroupElement) = {
    val $INFO$ = "A group element is encoded as a public key of Bitcoin in hex (compressed or uncompressed)"
    val $name$ = "g"
    val $groupElement$ = "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"
    $scala_env += name -> groupElement
    groupElement
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

  // ToDo: Test below
  def $setCollCollByte(name:String, collCollBytes:Array[Array[Byte]]) = {
    val $name$ = "d"
    val $collCollBytes$ = "[0x1a2b3c4d5e6f,0xafbecddc12,0xa132]"
    $scala_env += name -> collCollBytes
  }

  def deleteAll = $scala_env = Map()
  // use def for all get methods because $scala_env can be modified anytime and we need to use the latest one
  def getAll: Array[String] = $scala_env.toArray.map(EasyWebEncoder.encodeToString)
  def $get(key:String): Option[String] = $scala_env.get(key).map(value => EasyWebEncoder.encodeToString((key, value)))

  def $getEnv: Map[String, Any] = {
    $scala_env.map{ case (key, value) => key -> ScalaErgoConverters.getConvertedValue(value) }
  }

}
