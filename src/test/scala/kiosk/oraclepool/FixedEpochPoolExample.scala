package kiosk.oraclepool

import kiosk.ECC

object FixedEpochPoolExample extends FixedEpochPool {
  override lazy val livePeriod = 50 // blocks
  override lazy val prepPeriod = 10 // blocks
  override lazy val buffer = 4 // blocks

  override lazy val oracleTokenId: Array[Byte] = "dummy_oracle_token_id".getBytes()
  override lazy val poolTokenId: Array[Byte] = "dummy_pool_token_id".getBytes()

  override lazy val oracleReward = 150000000 // Nano ergs. One reward per data point to be paid to oracle
  override lazy val minPoolBoxValue = 1000000000 // how much min must exist in oracle pool box

  lazy val prvKey1 = ECC.$randBigInt
  lazy val pubKey1 = ECC.$gX(prvKey1)

  lazy val prvKey2 = ECC.$randBigInt
  lazy val pubKey2 = ECC.$gX(prvKey2)

  lazy val prvKey3 = ECC.$randBigInt
  lazy val pubKey3 = ECC.$gX(prvKey3)

  lazy val prvKey4 = ECC.$randBigInt
  lazy val pubKey4 = ECC.$gX(prvKey4)

  override lazy val oraclePubKeys = Array(pubKey1, pubKey2, pubKey3, pubKey4)
}
