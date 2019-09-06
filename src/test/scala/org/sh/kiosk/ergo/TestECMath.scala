package org.sh.kiosk.ergo

import java.math.BigInteger
import java.security.SecureRandom

import org.bouncycastle.util.BigIntegers
import org.sh.cryptonode.ecc.{ECCPrvKey, ECCPubKey, Point}
import sigmastate.basics.SecP256K1
import org.sh.cryptonode.ecc.Util._

object TestECMath extends App {
  val g_ergo = SecP256K1.generator
  val g_ergo_normalized = g_ergo.normalize()
  val g_cryptoNode = G

  assert(g_ergo_normalized.getXCoord.toBigInteger == g_cryptoNode.x.bigInteger)
  assert(g_ergo_normalized.getYCoord.toBigInteger == g_cryptoNode.y.bigInteger)

  val one = BigInteger.ONE
  val qMinusOne = SecP256K1.q.subtract(one)
  val randNum = BigIntegers.createRandomInRange(one, qMinusOne, new SecureRandom)

  val a_ergo = SecP256K1.exponentiate(g_ergo, randNum).normalize()
  val a_cryptoNode = g_cryptoNode * randNum

  assert(a_ergo.getXCoord.toBigInteger == a_cryptoNode.x.bigInteger)
  assert(a_ergo.getYCoord.toBigInteger == a_cryptoNode.y.bigInteger)
  val ergoScript = new ErgoScript{}
  val b_cryptoNode = new ECCPrvKey(randNum, true).eccPubKey.hex
  val b_ergo = ergoScript.$getGroupElement(randNum)

  assert(b_cryptoNode == b_ergo)
  val randNums = (1 to 1000).map {i =>
    BigIntegers.createRandomInRange(one, qMinusOne, new SecureRandom)
  }
  val t0 = System.currentTimeMillis()
  val c_cryptoNode = randNums.map {num =>
    g_cryptoNode * num
  }
  val t1 = System.currentTimeMillis()
  val c_ergo = randNums.map {num =>
    SecP256K1.exponentiate(g_ergo, num).normalize()
  }
  val t2 = System.currentTimeMillis()
  println(s"CryptoNode for ${randNums.size} elements ${t1-t0} millis")
  println(s"ErgoScript for ${randNums.size} elements ${t2-t1} millis")
  c_ergo zip c_cryptoNode foreach{
    case (d_ergo, d_cryptoNode) =>
      assert(d_ergo.getXCoord.toBigInteger == d_cryptoNode.x.bigInteger)
      assert(d_ergo.getYCoord.toBigInteger == d_cryptoNode.y.bigInteger)
  }
  println("ECMath tests passed")
}
