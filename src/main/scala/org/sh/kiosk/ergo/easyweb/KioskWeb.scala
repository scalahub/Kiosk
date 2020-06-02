package org.sh.kiosk.ergo.easyweb

import org.sh.kiosk.ergo.box.ErgoBox
import org.sh.kiosk.ergo.encoding.{EasyWebEncoder, ScalaErgoConverters}
import org.sh.kiosk.ergo.script.{ECC, ErgoScript, ErgoScriptEnv}
import org.sh.cryptonode.util.StringUtil._

object KioskWeb extends App {
  object Env extends ErgoScriptEnv
  object Box extends ErgoBox(Script)
  object Script extends ErgoScript(Env) {
    $myEnv.setCollByte("a", "f091616c10378d94b04ed7afb6e7e8da3ec8dd2a9be4a343f886dd520f688563".decodeHex)
    $myEnv.setBigInt("b", BigInt("123456789012345678901234567890123456789012345678901234567890"))
    $myEnv.setCollByte("c", "0x1a2b3c4d5e6f".decodeHex)
    $myEnv.setGroupElement("g", ScalaErgoConverters.stringToGroupElement("028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"))
  }
  val objects = List(
    Env,
    Script,
    ECC,
    Box
  )
  EasyWebEncoder
  new org.sh.easyweb.AutoWeb(objects, "KioskDemo")
}

