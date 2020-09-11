package kiosk

import java.security.SecureRandom

import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo._
import scorex.util.encode.Base58
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

  def hexToDecimal(hex:String) = BigInt(hex, 16)

  def hexToBase58(hex:String) = Base58.encode(hex.decodeHex)

  def hX(h:String, x:BigInt): String = {
    val $INFO$ =
      """Computes h^x for base h.
To set the default generator as base, use h = 0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798"""
    val $h$ = "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798"
    val $x$ = "123"
    val $isHex$ = "true"

    val h1: GroupElement = ScalaErgoConverters.stringToGroupElement(h)
    val hX: GroupElement = h1.exp(x.bigInteger)
    ScalaErgoConverters.groupElementToString(hX)
  }

  def $hexToBigInt(hex:String) = BigInt(hex, 16)

  def addressToGroupElement(address:String) = {
    /*
      encoding is as follows:

      group element
      ErgoTree serialized:      0008cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798
      group element:                  0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798
      group element serialized:     070279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798

      address:                     9fSgJ7BmUxBQJ454prQDQ7fQMBkXPLaAmDnimgTtjym6FYPHjAV
     */
    val ergoTree = ScalaErgoConverters.getAddressFromString(address).script.bytes.encodeHex
    if (ergoTree.size != 72) throw new Exception("Not a proveDlog address1")
    if (ergoTree.take(6) != "0008cd") throw new Exception("Not a proveDlog address2")
    ergoTree.drop(6)
  }
}

