package org.sh.kiosk.ergo.easyweb

import org.sh.kiosk.ergo.encoding.EasyWebEncoder
import org.sh.kiosk.ergo.script.ECC

object KioskWeb extends App {
  val objects = List(
    Env,
    Kiosk,
    ECC
  )
  EasyWebEncoder
  new org.sh.easyweb.AutoWeb(objects, "KioskDemo")
}

