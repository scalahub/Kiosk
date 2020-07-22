package kiosk.script

import org.ergoplatform.ErgoAddressEncoder.MainnetNetworkPrefix
import org.ergoplatform.{ErgoAddressEncoder, Pay2SAddress}
import org.sh.easyweb.Text
import org.sh.reflect.DataStructures.EasyMirrorSession
import sigmastate.Values.ErgoTree
import sigmastate.eval.CompiletimeIRContext
import sigmastate.lang.SigmaCompiler
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer

// Any variable/method starting with $ will not appear in EasyWeb front-end.
object KioskScriptCreator {
  val $networkPrefix = MainnetNetworkPrefix
  val $compiler = SigmaCompiler($networkPrefix)
  implicit val $irContext = new CompiletimeIRContext
  implicit val $ergoAddressEncoder: ErgoAddressEncoder = new ErgoAddressEncoder($networkPrefix)
}

class KioskScriptCreator(val $myEnv:KioskScriptEnv) extends EasyMirrorSession {

  import KioskScriptCreator._
  def getScriptHash(ergoScript:Text):Array[Byte] = {
    val $INFO$ = "Outputs the blake2b256 hash of the ErgoTree corresponding to ergoScript"
    val $ergoScript$:String = """{
  sigmaProp(1 < 2)
}"""

    scorex.crypto.hash.Blake2b256(getErgoTree(ergoScript)).toArray
  }

  def getErgoTree(ergoScript:Text):Array[Byte] = {
    val $INFO$ = "Outputs the blake2b256 hash of the ErgoTree corresponding to ergoScript"
    val $ergoScript$:String = """{
  sigmaProp(1 < 2)
}"""

    val ergoTree = $compile(ergoScript)
    DefaultSerializer.serializeErgoTree(ergoTree)
  }

  def $getP2SAddress(ergoScript:Text) = {
    val $ergoScript$ = """{
  sigmaProp(1 < 2)
}"""
    Pay2SAddress($compile(ergoScript)).toString
  }

  def $compile(ergoScript:Text):ErgoTree = {
    val $ergoScript$:String = """{
  sigmaProp(1 < 2)
}"""
    $compile(ergoScript.getText)
  }

  def $compile(ergoScript:String):ErgoTree = {
    import sigmastate.lang.Terms._
    $compiler.compile($myEnv.$getEnv.toMap, ergoScript).asSigmaProp // compiler.compile($myEnv.$getEnv, ergoScript).asInstanceOf[Value[SBoolean.type]].toSigmaProp
  }

  override def $setSession(sessionSecret: Option[String]): KioskScriptCreator = new KioskScriptCreator($myEnv.$setSession(sessionSecret))
}

