package org.sh.kiosk.ergo.encoding

import org.bouncycastle.math.ec.custom.sec.SecP256K1Point
import org.sh.cryptonode.ecc.ECCPubKey
import org.sh.cryptonode.util.StringUtil._
import org.sh.kiosk.ergo.{KioskErgoTree, KioskGroupElement}
import sigmastate.Values.ErgoTree
import sigmastate.basics.SecP256K1
import sigmastate.eval.SigmaDsl
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer
import special.sigma.GroupElement

object ScalaErgoConverters {

  def stringToGroupElement(hex:String): GroupElement = {
    val point = ECCPubKey(hex).point
    val secp256k1Point: SecP256K1Point = SecP256K1.createPoint(point.x.bigInteger, point.y.bigInteger)
    SigmaDsl.GroupElement(secp256k1Point)
  }

  def groupElementToString(groupElement: GroupElement): String = KioskGroupElement(groupElement).toString

  def stringToErgoTree(hex:String): ErgoTree = DefaultSerializer.deserializeErgoTree(hex.decodeHex)

  def ergoTreeToString(tree:ErgoTree): String = KioskErgoTree(tree).hex

}
