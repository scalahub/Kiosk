package kiosk

import kiosk.box.KioskBoxCreator
import kiosk.encoding.{EasyWebEncoder, ScalaErgoConverters}
import kiosk.script.{KioskScriptCreator, KioskScriptEnv}
import org.sh.cryptonode.util.StringUtil._

object Env extends KioskScriptEnv
object Box extends KioskBoxCreator(Script)
object Script extends KioskScriptCreator(Env) {
  $myEnv.setCollByte("a", "f091616c10378d94b04ed7afb6e7e8da3ec8dd2a9be4a343f886dd520f688563".decodeHex)
  $myEnv.setBigInt("b", BigInt("123456789012345678901234567890123456789012345678901234567890"))
  $myEnv.setCollByte("c", "0x1a2b3c4d5e6f".decodeHex)
  $myEnv.setCollGroupElement("d", Array("028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67",
    "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67",
    "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"))
  $myEnv.setGroupElement("g", "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67")
}

object KioskWeb extends App {
  val appInfo =
    """This is front-end for Kiosk, which is a library for interacting with the Ergo blockchain.
      |Kiosk is built on top of Ergo-AppKit and provides the ability to send transactions by spending
      |arbitrary boxes (provided their script allows them to be spent) and creating boxes with arbitrary guard scripts.
      |In order to create a script that hard-wires constants (such as a group element), first create a named constant
      |in the environment of the correct type. Then reference that name in the script code. To add that constant to a
      |register, use that name as part of the registerKeys array when creating a box.
      |
      |This is a multi-tenant version of Kiosk, where each URL uniquely determines an environment. Variables and
      |boxes in one environment are independent of those in other environments. Thus, multiple users may use Kiosk without
      |mixing up their environments as long as they use different URLs.
      |When visiting the bare base URL, the system automatically redirects to a randomly generated new URL.
      |
      |To get a new environment, simply use a new end-URL, either by visiting the bare base URL or manually creating one.
      |Note that environments are not saved to disk so the garbage collector may clear unused environments.""".stripMargin
  val objects = List(
    Env,
    Script,
    ECC,
    Box
  )
  EasyWebEncoder
  new org.sh.easyweb.AutoWebSession(objects, appInfo).generateWebXml
}

