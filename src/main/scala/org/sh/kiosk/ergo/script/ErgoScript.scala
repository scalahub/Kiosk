package org.sh.kiosk.ergo.script

import org.ergoplatform.ErgoAddressEncoder.MainnetNetworkPrefix
import org.ergoplatform.{ErgoAddressEncoder, Pay2SAddress}
import org.sh.easyweb.Text
import sigmastate.Values.ErgoTree
import sigmastate.eval.RuntimeIRContext
import sigmastate.lang.SigmaCompiler
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer

// Any variable/method starting with $ will not appear in EasyWeb front-end.
object ErgoScript {
  val networkPrefix = MainnetNetworkPrefix
  def compiler = SigmaCompiler(MainnetNetworkPrefix)
  implicit val irContext = new RuntimeIRContext // new CompiletimeIRContext
  implicit val ergoAddressEncoder: ErgoAddressEncoder = new ErgoAddressEncoder(MainnetNetworkPrefix)
}

class ErgoScript(val $myEnv:ErgoScriptEnv) {
  import ErgoScript._
  def getScriptHash(ergoScript:Text):Array[Byte] = {
    val $ergoScript$:String = """{
  val x = blake2b256(c)
  b > 1234.toBigInt &&
  a == x
}"""

    val ergoTree = compile(ergoScript)
    val scriptBytes = DefaultSerializer.serializeErgoTree(ergoTree)
    scorex.crypto.hash.Blake2b256(scriptBytes).toArray
  }

  def getP2sAddress(ergoScript:Text) = {
    val $ergoScript$ = """{
  val x = blake2b256(c)
  b > 1234.toBigInt &&
  a == x
}"""
    Pay2SAddress(compile(ergoScript)).toString
  }

  def compile(ergoScript:Text):ErgoTree = {
    val $ergoScript$:String = """{
  val x = blake2b256(c)
  b > 1234.toBigInt &&
  a == x
}"""
    $compile(ergoScript.getText)
  }

  def $compile(ergoScript:String):ErgoTree = {
    import sigmastate.lang.Terms._
    compiler.compile($myEnv.$getEnv, ergoScript).asSigmaProp //compiler.compile($myEnv.$getEnv, ergoScript).asInstanceOf[Value[SBoolean.type]].toSigmaProp
  }

}

