package org.sh.kiosk.ergo.easyweb

import org.sh.kiosk.ergo.fullnode.API
import org.sh.kiosk.ergo.script.ECC

object Admin extends App {
  val objects = List(
    Env,
    Kiosk,
    API,
    ECC
  )
  new org.sh.easyweb.AutoWeb(objects, "KioskDemo")
}

