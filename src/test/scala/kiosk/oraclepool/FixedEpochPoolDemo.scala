package kiosk.oraclepool

import kiosk.ECC
import kiosk.ergo._

object FixedEpochPoolDemo extends FixedEpochPool with App {
  val minBoxValue = 2000000
  override lazy val livePeriod = 4 // blocks
  override lazy val prepPeriod = 4 // blocks
  override lazy val buffer = 2 // blocks

  override lazy val oracleTokenId: Array[Byte] = "12caaacb51c89646fac9a3786eb98d0113bd57d68223ccc11754a4f67281daed".decodeHex
  override lazy val poolTokenId: Array[Byte] = "961c8d498431664f4fb8a660b9a62618f092e34ef07370ba1a2fb7c278c5f57d".decodeHex

  override lazy val oracleReward = 2000000 // Nano ergs. One reward per data point to be paid to oracle
  lazy val addresses = Seq(
    "9iHunbPfq8ARpiJXc4vjmwvseeHWjmgeC797vSrdHSLNKxvKsYo",
    "9fj9NJpzo13HfNyCdzyfNP8zAfjiTY3pys1JP5wCzez8MiP8QbF",
    "9ebeQK9oJpDpTZSfqk6wdaHt3x1aUUba9S8dMufTpyQQYvE2XKU",
    "9fcrXXaJgrGKC8iu98Y2spstDDxNccXSR9QjbfTvtuv7vJ3NQLk"
  ).toArray

  override lazy val minPoolBoxValue = oracleReward * (addresses.size + 1) + minBoxValue // how much min must exist in oracle pool box
  override lazy val oraclePubKeys = addresses.map(ECC.addressToGroupElement)

  println("minPoolBoxValue " + minPoolBoxValue)
  println(s"Live Epoch script length       : ${liveEpochErgoTree.bytes.length}")
  println(s"Live Epoch script complexity   : ${liveEpochErgoTree.complexity}")
  println(s"Epoch prep script length       : ${epochPrepErgoTree.bytes.length}")
  println(s"Epoch prep script complexity   : ${epochPrepErgoTree.complexity}")
  println(s"DataPoint script length        : ${dataPointErgoTree.bytes.length}")
  println(s"DataPoint script complexity    : ${dataPointErgoTree.complexity}")
  println(s"PoolDeposit script length      : ${poolDepositErgoTree.bytes.length}")
  println(s"PoolDeposit script complexity  : ${poolDepositErgoTree.complexity}")

  println("liveEpochAddress: " + liveEpochAddress)
  println("epochPrepAddress: " + epochPrepAddress)
  println("dataPointAddress: " + dataPointAddress)
  println("poolDepositAddress: " + poolDepositAddress)

  println("EpochPrepErgoTree: (for R6) " + KioskErgoTree(epochPrepErgoTree).serialize.encodeHex)

}
