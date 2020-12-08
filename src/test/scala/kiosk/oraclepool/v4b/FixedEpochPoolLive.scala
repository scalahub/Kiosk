package kiosk.oraclepool.v4b

import kiosk.ergo._
import org.scalatest.{Matchers, PropSpec}
import scorex.crypto.hash.Blake2b256

trait FixedEpochPoolLive extends FixedEpochPool {
  lazy val minBoxValue = 2000000 // min value to remain AFTER paying rewards. For min value to keep BEFORE rewards, see minPoolBoxValue
  override def livePeriod = 20 // blocks
  override def prepPeriod = 10 // blocks
  override def buffer = 4 // blocks
  override def maxDeviation: Int = 5 // percent
  override def minOracleBoxes: Int = 3 // percent

  // 10 tokens issued
  // https://explorer.ergoplatform.com/en/transactions/4839dbe2abb09d847623d65a94946518424e13f6e4163ca727e64c69132b1b9e
  lazy val oracleToken = "f0ff151ed1250fb305a74bf843f8de1be6a13e3d1a37229b94391e648357a4ca"

  // 1 token issued
  // https://explorer.ergoplatform.com/en/transactions/2c063280409b74010106a1412205304a8dad4e73da845d32bd7e9d1d32fe77ec
  lazy val poolToken = "323b13f731b00bbf33899f00543f851b20d794c21e9b81ba5f71cec8289c0619"

  override def oracleTokenId: Array[Byte] = oracleToken.decodeHex
  override def poolTokenId: Array[Byte] = poolToken.decodeHex

  override def oracleReward = 2000000 // Nano ergs. One reward per data point to be paid to oracle
  lazy val maxNumOracles = 10

  override def minPoolBoxValue: Amount = oracleReward * (maxNumOracles + 1) + minBoxValue // min value allowed in live epoch box
}
