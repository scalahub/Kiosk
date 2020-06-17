package kiosk

import java.security.SecureRandom

import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.math.ec.custom.sec.SecP256K1Point
import org.sh.cryptonode.ecc.{ECCPubKey, Point}
import sigmastate.basics.SecP256K1

object ECC {
  private def toPubKey(u:ECPoint) = {
    val uX = u.getXCoord.toBigInteger
    val uY = u.getYCoord.toBigInteger
    ECCPubKey(Point(uX, uY), true).hex
  }
  private def gExpX(g:SecP256K1Point, x:BigInt) = {
    toPubKey(SecP256K1.exponentiate(g, x.bigInteger).normalize())
  }

  def gX(x:BigInt): String = {
    val $x$ = "123"
    val $INFO$ = "Computes g^x for default generator g"
    gExpX(SecP256K1.generator, x)
  }

  def $randBigInt: BigInt = {
    val random = new SecureRandom()
    val values = new Array[Byte](32)
    random.nextBytes(values)
    BigInt(values).mod(SecP256K1.q)
  }

  def hX(h:String, x:BigInt): String = {
    val $INFO$ = "Computes h^x for supplied generator h"
    val $h$ = "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"
    val $x$ = "123"
    val point = ECCPubKey(h).point
    val base: SecP256K1Point = SecP256K1.createPoint(point.x.bigInteger, point.y.bigInteger)
    gExpX(base, x)
  }

  def $hexToBigInt(hex:String) = BigInt(hex, 16)
}
