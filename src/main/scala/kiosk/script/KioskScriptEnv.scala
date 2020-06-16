package kiosk.script

import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo._
import org.sh.cryptonode.util.BytesUtil._
import org.sh.reflect.DataStructures.EasyMirrorSession
import special.sigma.GroupElement
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
    val $name$ = "g"
    val $groupElement$ = "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"
    $envMap += name -> KioskGroupElement(ScalaErgoConverters.stringToGroupElement(groupElement))
  }

  def setCollGroupElement(name:String, coll: Array[String]):Unit = {
    val $INFO$ = "A group element is encoded as a public key of Bitcoin in hex (compressed or uncompressed)"
    val $name$ = "d"
    val $coll$ = "[028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67,028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67,028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67]"
    val groupElements = coll.map(ScalaErgoConverters.stringToGroupElement)
    $envMap += name -> KioskCollGroupElement(groupElements)
  }

  def setBigInt(name:String, bigInt:BigInt):Unit = {
    val $name$ = "b"
    val $bigInt$ = "1234567890123456789012345678901234567890"
    $envMap += name -> KioskBigInt(bigInt)
  }

  def setLong(name:String, long:Long):Unit = {
    val $name$ = "long"
    val $long$ = "12345678901112"
    $envMap += name -> KioskLong(long)
  }

  def setInt(name:String, int:Int):Unit = {
    val $name$ = "int"
    val $int$ = "123456789"
    $envMap += name -> KioskInt(int)
  }

  def setCollByte(name:String, bytes:Array[Byte]):Unit = {
    val $name$ = "c"
    val $collBytes$ = "0x1a2b3c4d5e6f"
    $envMap += name -> KioskCollByte(bytes)
  }

  def setString(name:String, string:String):Unit = {
    val $name$ = "string"
    val $string$ = "Nothing backed USD token"
    $envMap += name -> KioskCollByte(string.getBytes("UTF-8"))
  }

  def deleteAll:Unit = $envMap.clear()

  def getAll(serialize:Boolean): Array[String] = $envMap.toArray.map{
    case (key, kioskType) =>
      val string = if (serialize) kioskType.serialize.encodeHex else kioskType.toString
      s"""{"name":"$key", "value":"${string}", "type":"${kioskType.typeName}"}"""
  }

  def $getEnv: MMap[String, Any] = {
    $envMap.map{ case (key, kioskType) => key -> kioskType.value }
  }

  override def $setSession(sessionSecret: Option[String]): KioskScriptEnv = {
    new KioskScriptEnv(sessionSecret)
  }
}
