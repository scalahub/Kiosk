package kiosk.oraclepool.v4a

import kiosk.ergo._

trait OraclePoolLive extends OraclePool {
  lazy val minBoxValue = 2000000 // min value to remain AFTER paying rewards. For min value to keep BEFORE rewards, see minPoolBoxValue
  override def livePeriod = 20 // blocks
  override def prepPeriod = 10 // blocks
  override def buffer = 4 // blocks
  override def maxDeviation: Int = 5 // percent
  override def minOracleBoxes: Int = 3 // percent

  lazy val oracleToken = "749fe0b8c63213be3451af2578eacabd620a9e687f5c55c54f1ec571b17c9c85"
  lazy val poolToken = "0fb1eca4646950743bc5a8c341c16871a0ad9b4077e3b276bf93855d51a042d1"

  override def oracleTokenId: Array[Byte] = oracleToken.decodeHex

  override def poolTokenId: Array[Byte] = poolToken.decodeHex

  override def oracleReward = 20000000 // Nano ergs. One reward per data point to be paid to oracle
  lazy val maxNumOracles = 10

  override def minPoolBoxValue: Amount = oracleReward * (maxNumOracles + 1) + minBoxValue // min value allowed in live epoch box
}
