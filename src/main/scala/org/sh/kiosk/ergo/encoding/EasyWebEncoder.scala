package org.sh.kiosk.ergo.encoding

import org.sh.kiosk.ergo.encoding.ScalaErgoConverters.{ergoTreeToString, groupElementToString, stringToErgoTree, stringToGroupElement}
import org.sh.reflect.DefaultTypeHandler
import sigmastate.Values.ErgoTree
import special.sigma.GroupElement

object EasyWebEncoder {
  DefaultTypeHandler.addType[GroupElement](classOf[GroupElement], stringToGroupElement, groupElementToString)
  DefaultTypeHandler.addType[ErgoTree](classOf[ErgoTree], stringToErgoTree, ergoTreeToString)
}
