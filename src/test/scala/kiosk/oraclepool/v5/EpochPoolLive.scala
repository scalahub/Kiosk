package kiosk.oraclepool.v5

import kiosk.ergo._
import org.scalatest.{Matchers, PropSpec}
import scorex.crypto.hash.Blake2b256

trait EpochPoolLive extends EpochPool {
  lazy val minBoxValue = 2000000 // min value to remain AFTER paying rewards. For min value to keep BEFORE rewards, see minPoolBoxValue
  override def livePeriod = 20 // blocks
  override def prepPeriod = 10 // blocks
  override def buffer = 4 // blocks
  override def maxDeviation: Int = 5 // percent
  override def minOracleBoxes: Int = 4

  lazy val oracleToken = "749fe0b8c63213be3451af2578eacabd620a9e687f5c55c54f1ec571b17c9c85"
  lazy val poolToken = "0fb1eca4646950743bc5a8c341c16871a0ad9b4077e3b276bf93855d51a042d1"
  lazy val updateNFT = "77d14a018507949d1a88a631f76663e8e5101f57305dd5ebd319a41028d80456".decodeHex
  lazy val ballotTokenId = "dd26438230986cfe7305ad958451b69e55ad5ac37c8a355bfb08d810edd7a20f".decodeHex

  override def oracleTokenId: Array[Byte] = oracleToken.decodeHex
  override def poolNFT: Array[Byte] = poolToken.decodeHex

  override def oracleReward = 20000000 // Nano ergs. One reward per data point to be paid to oracle
  lazy val maxNumOracles = 12
  lazy val minVotes = 6
  lazy val minStorageRent = 10000000L

  override def minPoolBoxValue: Amount = oracleReward * (maxNumOracles + 1) + minBoxValue // min value allowed in live epoch box
}
