package kiosk.oraclepool.v2

import kiosk.ergo._
import org.scalatest.{Matchers, PropSpec}
import scorex.crypto.hash.Blake2b256

class FixedEpochPoolDemo extends PropSpec with FixedEpochPool with Matchers {
  val minBoxValue = 2000000
  override lazy val livePeriod = 5 // blocks
  override lazy val prepPeriod = 5 // blocks
  override lazy val buffer = 4 // blocks

  override lazy val oracleTokenId: Array[Byte] = "12caaacb51c89646fac9a3786eb98d0113bd57d68223ccc11754a4f67281daed".decodeHex
  override lazy val poolTokenId: Array[Byte] = "b662db51cf2dc39f110a021c2a31c74f0a1a18ffffbf73e8a051a7b8c0f09ebc".decodeHex

  override lazy val oracleReward = 2000000 // Nano ergs. One reward per data point to be paid to oracle
  lazy val addresses = Seq(
    "9iHunbPfq8ARpiJXc4vjmwvseeHWjmgeC797vSrdHSLNKxvKsYo",
    "9fj9NJpzo13HfNyCdzyfNP8zAfjiTY3pys1JP5wCzez8MiP8QbF",
    "9ebeQK9oJpDpTZSfqk6wdaHt3x1aUUba9S8dMufTpyQQYvE2XKU",
    "9fcrXXaJgrGKC8iu98Y2spstDDxNccXSR9QjbfTvtuv7vJ3NQLk"
  ).toArray

  override lazy val minPoolBoxValue = oracleReward * (addresses.size + 1) + minBoxValue // how much min must exist in oracle pool box

  property("Display Addresses") {
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
    println("EpochPrepScriptHash: (for R6 of LiveEpoch box) " + Blake2b256(epochPrepErgoTree.bytes).encodeHex)
    println("LiveEpochScriptHash: (for hardwiring in Datapoint box) " + Blake2b256(liveEpochErgoTree.bytes).encodeHex)
    println("Min box value: "+minPoolBoxValue)

    Blake2b256(KioskErgoTree(epochPrepErgoTree).serialize).encodeHex shouldEqual Blake2b256(epochPrepErgoTree.bytes).encodeHex

    liveEpochAddress shouldEqual "3vThpSDoLo58CtKKFLBQMmtcD5e5pJeFNNyPKnDRC4zKzhgySeTUkU71fk9mcFgHe23k1b4QuERNdcignnexcULMEenifBffiNeCdiTkgaUiGtH5D9rrsj698mRLDhANmybx8c6NunwUMoKuLsRoEYtYi8rRjuKfbNDN1HfVsgFKSyKMSnwJXa5KAuABSz5dYUgURf6M3i2bxsKKYTe4uQFEoVcbBwvfW4UxXaKqQYGB8xGLASMfHtcs9R5CBFkHyUSXh2sFy17pfdQ5emx8CgE5ZXRqx7YBYzk9jSyGqp2myT5XvBAS2uSeahNKWYKzh1XTqDc3YGLvBPHJ98bksaaSnNX4SwAhia2mXY4iCKsYf6F7p5QPNjYBXqLyzkDFxSzgQJmMg1Ybh3fx6Sg8esE9w5L7KCGEuydPkBE"
    epochPrepAddress shouldEqual "Gxd4hMRT6J1SA6D3tfvyij49J2DCQkeZfxNVEpoZidZtS9YYsi8Jg5u3JBZQHxdmrLpVgTsnLnSbt377BRJAWFUfkdcmC1pMPFNUYBWuYaccbMxP5kV3WkGU7oxsWJauKfiGkFZPN1W1RmWVmpFbdKaCizjnMqC7TLsQ53JfBzWo5CsYj2Vn3YYbJFZiXbfVXWKjvkUHatcGxL47QnBffcKfFJun7t1tFgxowLonpFpq7SFAz4YRE6TdZarmWDjDER13pSUupfaKCZmUe3aCRhgAsdp4RHuW8n1RywcYcSjGNPVFzsGjD8GQdUrs85Xv4gobuH49S4WZFgkcoQAx3jx3GqhY9kQWwdn7Ni7v2XcKMwFFCvvzrPAKtUHLZYU4VN4RjvoFLRYJ5H"
    dataPointAddress shouldEqual "jL2aaqw6XU61SZznxeykLpREPzSmZv8bwbjEsJD6DMfXQLgBc12wMmPpVD81JnLxfxkT6s5nvYgbB62vkH8ChHeuVKtCPDMLTZ3gFMTa11YXXGBKvkezBENzpDBh8HsLHhnTTbMzv2sViDQpSWVNEF6G3Z9Fn2Ce6TNc5iHFZr7jGCBLtfRLKMb9RRUc9voWz9yEWpgADEkoQnDyMn5wc6xLoJsSYLfXHo2t8pyvwXfn2NotR3xFRDHU7wHXe"
    poolDepositAddress shouldEqual "zLSQDVBaFJVVPWsvzN8begiciWsjdiFyJn9NwnLbJxMrGehDXPJnEuWm2x8gQtCutoK7crMSP9sKQBPyaPVRQXpiSr7ZoKrz4arYiJXKX1MDAfJFm9tjkY379ZiskLYHC3mmf4CQxATbY9P3mTjYw3f3Hkoxnu4yxvMCVBtRTuuRK1qh4E6aGpG8cJcpJ5qBtEsx7SrJoMZP34exMNxD1dPoaDFbuKHnoXAZmDLHnLqG3HgdPy"
    Blake2b256(epochPrepErgoTree.bytes).encodeHex shouldEqual "5ea046c8753cbf8bb0acdbd67dd8a5d905df89d67060624282ad757fa3cb670c"
    Blake2b256(liveEpochErgoTree.bytes).encodeHex shouldEqual "955fd2c22393aa0f5db841dd8a3ad44ebb7de970419f5a0a58441ebe6b809fb2"
    minPoolBoxValue shouldEqual 10000000
  }
}
