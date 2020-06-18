package kiosk

import java.security.SecureRandom

import kiosk.encoding.ScalaErgoConverters
import sigmastate.basics.SecP256K1
import sigmastate.eval._
import special.sigma.GroupElement

object ECC {
  def $gX(x:BigInt): String = {
    val $x$ = "123"
    val $INFO$ = "Computes g^x for default generator g"
    val g = SigmaDsl.GroupElement(SecP256K1.generator)
    val gX = g.exp(x.bigInteger)
    ScalaErgoConverters.groupElementToString(gX)
  }

  def $randBigInt: BigInt = {
    val random = new SecureRandom()
    val values = new Array[Byte](32)
    random.nextBytes(values)
    BigInt(values).mod(SecP256K1.q)
  }

  def hX(h:String, x:BigInt): String = {
    val $INFO$ =
      """Computes h^x for base h.
To set the default generator as base, use h = 0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798"""
    val $h$ = "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798"
    val $x$ = "123"

    val h1: GroupElement = ScalaErgoConverters.stringToGroupElement(h)
    val hX: GroupElement = h1.exp(x.bigInteger)
    ScalaErgoConverters.groupElementToString(hX)
  }

  def $hexToBigInt(hex:String) = BigInt(hex, 16)
}
