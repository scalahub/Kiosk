package org.sh.kiosk.ergo

import java.math.BigInteger
import java.security.SecureRandom

import org.bouncycastle.util.BigIntegers
import org.sh.cryptonode.ecc.{ECCPrvKey, ECCPubKey, Point}
import sigmastate.basics.SecP256K1
import org.sh.cryptonode.ecc.Util._

object TestECMath extends App {
  val g = SecP256K1.generator

  val g1 = g.normalize()
  val g2 = G
  assert(g1.getXCoord.toBigInteger == g2.x.bigInteger)
  assert(g1.getYCoord.toBigInteger == g2.y.bigInteger)

  val one = BigInteger.ONE
  val qMinusOne = SecP256K1.q.subtract(one)
  val randNum = BigIntegers.createRandomInRange(one, qMinusOne, new SecureRandom)

  val a = SecP256K1.exponentiate(g, randNum).normalize()
  val b = G * randNum

  assert(a.getXCoord.toBigInteger == b.x.bigInteger)
  assert(a.getYCoord.toBigInteger == b.y.bigInteger)

  val c = new ECCPrvKey(randNum, true).eccPubKey.hex
  val d = ErgoScript.getGroupElement(randNum)
  assert(c == d)
}
