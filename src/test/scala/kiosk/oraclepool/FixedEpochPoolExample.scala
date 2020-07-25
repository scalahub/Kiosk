package kiosk.oraclepool

object FixedEpochPoolExample extends FixedEpochPool {
  override lazy val livePeriod = 50 // blocks
  override lazy val prepPeriod = 10 // blocks
  override lazy val buffer = 4 // blocks

  override lazy val oracleTokenId: Array[Byte] = "dummy_oracle_token_id".getBytes()
  override lazy val poolTokenId: Array[Byte] = "dummy_pool_token_id".getBytes()

  override lazy val oracleReward = 150000000 // Nano ergs. One reward per data point to be paid to oracle
  override lazy val minPoolBoxValue = 1000000000 // how much min must exist in oracle pool box
}
