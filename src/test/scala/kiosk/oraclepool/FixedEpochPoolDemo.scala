package kiosk.oraclepool

import kiosk.ECC

object FixedEpochPoolDemo extends FixedEpochPool {
  override lazy val livePeriod = 50 // blocks
  override lazy val prepPeriod = 10 // blocks
  override lazy val buffer = 4 // blocks

  // ToDo: replace below two byte arrays with actual token Ids
  override lazy val oracleTokenId: Array[Byte] = Array.fill(32)(0x00)
  override lazy val poolTokenId: Array[Byte] = Array.fill(32)(0x01)

  override lazy val oracleReward = 150000000 // Nano ergs. One reward per data point to be paid to oracle
  override lazy val minPoolBoxValue = 1000000000 // how much min must exist in oracle pool box

  lazy val addresses = Seq(
    "9iHunbPfq8ARpiJXc4vjmwvseeHWjmgeC797vSrdHSLNKxvKsYo",
    "9fj9NJpzo13HfNyCdzyfNP8zAfjiTY3pys1JP5wCzez8MiP8QbF",
    "9ebeQK9oJpDpTZSfqk6wdaHt3x1aUUba9S8dMufTpyQQYvE2XKU"
  ).toArray

  override lazy val oraclePubKeys = addresses.map(ECC.addressToGroupElement)
}
