package kiosk.script

import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo._
import org.sh.cryptonode.util.BytesUtil._
import special.sigma.GroupElement

class ErgoScriptEnv {

  // Any variable starting with '$' is hidden from the auto-generated frontend of EasyWeb
  var $envMap:Map[String, KioskType[_]] = Map()

  def setGroupElement(name:String, groupElement: GroupElement) = {
    val $INFO$ = "A group element is encoded as a public key of Bitcoin in hex (compressed or uncompressed)"
    val $name$ = "g"
    val $groupElement$ = "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"
    $envMap += name -> KioskGroupElement(groupElement)
    groupElement
  }

  def setCollGroupElement(name:String, coll: Array[String]) = {
    val $INFO$ = "A group element is encoded as a public key of Bitcoin in hex (compressed or uncompressed)"
    val $name$ = "d"
    val $coll$ = "[028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67,028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67,028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67]"
    val groupElements = coll.map(ScalaErgoConverters.stringToGroupElement)
    $envMap += name -> KioskCollGroupElement(groupElements)
    groupElements.map(ScalaErgoConverters.groupElementToString)
  }

  def setBigInt(name:String, bigInt:BigInt) = {
    val $name$ = "b"
    val $bigInt$ = "1234567890123456789012345678901234567890"
    $envMap += name -> KioskBigInt(bigInt)
  }

  def setLong(name:String, long:Long) = {
    val $name$ = "long"
    val $long$ = "12345678901112"
    $envMap += name -> KioskLong(long)
  }

  def setInt(name:String, int:Int) = {
    val $name$ = "int"
    val $int$ = "123456789"
    $envMap += name -> KioskInt(int)
  }

  def setCollByte(name:String, bytes:Array[Byte]) = {
    val $name$ = "c"
    val $collBytes$ = "0x1a2b3c4d5e6f"
    $envMap += name -> KioskCollByte(bytes)
  }

  def setString(name:String, string:String) = {
    val $name$ = "string"
    val $string$ = "Nothing backed USD token"
    $envMap += name -> KioskCollByte(string.getBytes("UTF-8"))
  }

  def deleteAll = $envMap = Map()

  def getAll(serialize:Boolean): Array[String] = $envMap.toArray.map{
    case (key, kioskType) =>
      val string = if (serialize) kioskType.serialize.encodeHex else kioskType.toString
      s"""{"name":"$key", "value":"${string}", "type":"${kioskType.typeName}"}"""
  }

  def $getEnv: Map[String, Any] = {
    $envMap.map{ case (key, kioskType) => key -> kioskType.value }
  }
}
