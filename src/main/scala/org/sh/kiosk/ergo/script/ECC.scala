package org.sh.kiosk.ergo.script

import java.security.SecureRandom

import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.math.ec.custom.sec.SecP256K1Point
import org.sh.cryptonode.ecc.{ECCPubKey, Point}
import org.sh.cryptonode.util.BytesUtil._
import sigmastate.basics.SecP256K1
import special.sigma.GroupElement

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
    val $INFO$ = "Computes g^x for default generator g"
    gExpX(SecP256K1.generator, x)
  }

  def $randBigInt: BigInt = {
    val random = new SecureRandom()
    val values = new Array[Byte](32)
    random.nextBytes(values)
    BigInt(values).mod(SecP256K1.q)
  }

  def hX(h:GroupElement, x:BigInt): String = {
    val $INFO$ = "Computes h^x for supplied generator h"
    val point = ECCPubKey(h.getEncoded.toArray.encodeHex).point
    val base: SecP256K1Point = SecP256K1.createPoint(point.x.bigInteger, point.y.bigInteger)
    gExpX(base, x)
  }

}

