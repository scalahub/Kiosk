package kiosk.script

import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo._
import org.sh.reflect.DataStructures.EasyMirrorSession
import org.sh.utils.json.JSONUtil.JsonFormatted

import scala.collection.mutable.{Map => MMap}

object KioskScriptEnv{
  private val sessionSecretEnvMap:MMap[String, MMap[String, KioskType[_]]] = MMap()

  private def envMap(sessionSecret:Option[String]):MMap[String, KioskType[_]] = {
    sessionSecret match {
      case None => MMap()
      case Some(secret) =>
        sessionSecretEnvMap.get(secret) match {
          case Some(map) => map
          case _ => sessionSecretEnvMap += secret -> MMap()
            sessionSecretEnvMap(secret)
        }
    }
  }
}

class KioskScriptEnv(val $sessionSecret:Option[String] = None) extends EasyMirrorSession {
  import KioskScriptEnv._

  // Any variable starting with '$' is hidden from the auto-generated frontend of EasyWeb
  val $envMap:MMap[String, KioskType[_]] = envMap($sessionSecret)

  def setGroupElement(name:String, groupElement: String):Unit = {
    val $INFO$ = "A group element is encoded as a public key of Bitcoin in hex (compressed or uncompressed)"
    val $name$ = "myGroupElement"
    val $groupElement$ = "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"
    $addIfNotExist(name, KioskGroupElement(ScalaErgoConverters.stringToGroupElement(groupElement)))
  }

  def setCollGroupElement(name:String, coll: Array[String]):Unit = {
    val $INFO$ = "A group element is encoded as a public key of Bitcoin in hex (compressed or uncompressed)"
    val $name$ = "myCollGroupElement"
    val $coll$ = "[028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67,028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67,028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67]"
    val groupElements = coll.map(ScalaErgoConverters.stringToGroupElement)
    $addIfNotExist(name, KioskCollGroupElement(groupElements))
  }

  def decodeValue(hex:String) = {
    val kioskType = ScalaErgoConverters.deserialize(hex)
    s"${kioskType.typeName}: ${kioskType.toString}"
  }

  def setBigInt(name:String, bigInt:BigInt):Unit = {
    val $name$ = "myBigInt"
    val $bigInt$ = "1234567890123456789012345678901234567890"
    $addIfNotExist(name, KioskBigInt(bigInt))
  }

  def setLong(name:String, long:Long):Unit = {
    val $name$ = "myLong"
    val $long$ = "12345678901112"
    $addIfNotExist(name, KioskLong(long))
  }

  def setInt(name:String, int:Int):Unit = {
    val $name$ = "myInt"
    val $int$ = "123456789"
    $addIfNotExist(name, KioskInt(int))
  }

  def setCollByte(name:String, bytes:Array[Byte]):Unit = {
    val $name$ = "myCollByte"
    val $bytes$ = "0x1a2b3c4d5e6f"
    $addIfNotExist(name, KioskCollByte(bytes))
  }

  def $addIfNotExist(name:String, kioskType: KioskType[_]) = {
    $envMap.get(name).fold(
      $envMap += name -> kioskType
    )(_ => throw new Exception(s"Variable $name is already defined"))
  }

  def setString(name:String, string:String):Unit = {
    val $name$ = "myString"
    val $string$ = "Nothing backed USD token"
    $addIfNotExist(name,  KioskCollByte(string.getBytes("UTF-8")))
  }

  def deleteAll(reallyDelete:Boolean) = {
    val $INFO$ = "To prevent accidental clicking, please select 'yes' from the radio button"
    val $reallyDelete$ = "false"
    if (reallyDelete) {
      $envMap.clear()
      "Deleted all environment variables"
    } else {
      "Please set reallyDelete to yes to delete environment variables"
    }
  }

  def getAll(serialize:Boolean): Array[String] = $envMap.toArray.map{
    case (key, kioskType) =>
      val value = if (serialize) kioskType.serialize.encodeHex else kioskType.toString
      new JsonFormatted {
        override val keys: Array[String] = Array("name", "value", "type")
        override val vals: Array[Any] = Array(key, value, kioskType.typeName)
      }.toString
  }

  def $getEnv: MMap[String, Any] = {
    $envMap.map{ case (key, kioskType) => key -> kioskType.value }
  }

  override def $setSession(sessionSecret: Option[String]): KioskScriptEnv = {
    new KioskScriptEnv(sessionSecret)
  }
}
