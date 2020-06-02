package org.sh.kiosk.ergo.encoding

import ScalaErgoConverters.{ergoTreeToHex, groupElementToHex, hexToErgoTree, hexToGroupElement}
import org.sh.reflect.DefaultTypeHandler
import sigmastate.Values.ErgoTree
import special.sigma.GroupElement
import org.sh.cryptonode.util.BytesUtil._
import org.sh.kiosk.ergo.fullnode.ReqType

object EasyWebEncoder {
  DefaultTypeHandler.addType[GroupElement](classOf[GroupElement], hexToGroupElement, groupElementToHex)
  DefaultTypeHandler.addType[ErgoTree](classOf[ErgoTree], hexToErgoTree, ergoTreeToHex)
  DefaultTypeHandler.addType[ReqType](classOf[ReqType], ReqType.fromString, reqType => reqType.value)


  def encodeToString(nameValue:(String, Any)): String = {
    val (name, value) = nameValue
    val (elValue, elType) = value match {
      case grp: GroupElement => (grp.getEncoded.toArray.encodeHex, "GroupElement")
      case bigInt: BigInt => (bigInt.toString(10), "BigInt")
      case arrayByte: Array[Byte] => (DefaultTypeHandler.typeToString(arrayByte.getClass, arrayByte), "Coll[Byte]")
      case arrayArrayByte: Array[Array[Byte]] => (DefaultTypeHandler.typeToString(arrayArrayByte.getClass, arrayArrayByte), "Coll[Coll[Byte]]")
      case any => (any.toString, any.getClass)
    }
    s"""{"name":"$name", "value":"${elValue}", "type":"${elType}"}"""
  }

}
