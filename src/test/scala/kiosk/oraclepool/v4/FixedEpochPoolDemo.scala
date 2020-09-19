package kiosk.oraclepool.v4

import kiosk.ergo._
import org.scalatest.{Matchers, PropSpec}
import scorex.crypto.hash.Blake2b256

class FixedEpochPoolDemo extends PropSpec with FixedEpochPool with Matchers {
  lazy val minBoxValue = 2000000 // min value to remain AFTER paying rewards. For min value to keep BEFORE rewards, see minPoolBoxValue
  override def livePeriod = 20 // blocks
  override def prepPeriod = 10 // blocks
  override def buffer = 4 // blocks
  override def maxDeviation: Int = 5 // percent
  override def minOracleBoxes: Int = 2 // percent

  override def oracleTokenId: Array[Byte] = "12caaacb51c89646fac9a3786eb98d0113bd57d68223ccc11754a4f67281daed".decodeHex
  override def poolTokenId: Array[Byte] = "b662db51cf2dc39f110a021c2a31c74f0a1a18ffffbf73e8a051a7b8c0f09ebc".decodeHex

  override def oracleReward = 2000000 // Nano ergs. One reward per data point to be paid to oracle
  lazy val maxNumOracles = 20

  override def minPoolBoxValue: Amount = oracleReward * (maxNumOracles + 1) + minBoxValue // min value allowed in live epoch box

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
    println("EpochPrepScriptHash: (for R6 of LiveEpochBox) " + Blake2b256(KioskErgoTree(epochPrepErgoTree).serialize).encodeHex)
    println("LiveEpochScriptHash: (for hard-coding in Datapoint script) "+Blake2b256(liveEpochErgoTree.bytes).encodeHex)
    println("Min box value: "+minPoolBoxValue)

    liveEpochAddress shouldEqual "8D6pdYVRvF6NPZ67GyFpkosDVqNpBuBVmZaYM9m2TAvM172T5WmePLzrs8Fbr1r1UmJBEeYa18wU92uUXngG4oGf5jZCx7rSpG2ydpr3T3Mey9VtDEshJS3ub3Ag3WcmQCnbX23JUYbtTk34kpkpWrn8q5XDV6DWbtdZNNgwp8QpdQBFKpQX8Symhcrh8fRUqdpXmHAsggRHEMMNxHqSSAh8caTUzjMaEeQKRW1xLDX9Mwj3KbEeR6LsbBZvBCPQKuZ3RXXejoBny4StBmtraLjP87pLJSRwufb84vzyXe9ZFDK97kwBVMaj9fEYjVinCNdBLHUDQoeTwkFhuBci4ZEBpZTPczTRHDhsW7Z5XLCHJGNqizi9aC849smECn9tCyWdp3bnKXY2c1pYVmYNQkbt3ny65PucQoiTfwq9moHs8da4c4Sx4eYN4YpEwPqjEckUwaqkhq7gQtB2aTACRFvaXeuNf3fTxnN9Qo8z4kPrRzZKMMJSYN2i4bZCbMtyXNwfunqdA5Q2z72hm5npNbfPZ48yNjDEF42WRsbswg2MYUvX2v576U7nT8tyT"
    epochPrepAddress shouldEqual "Gxd4hMRT5aE7dxAoMpvcP1G5cnu2snDfDoToVhtMzx1vUHmQtsMPMauXZ38p2j3hDmdAuGqu1uA94ULrFPGnhKvfKLuvDjsxHyoXXJJQzxQHGJSMUjvGmhGJNThMd7FoXsZqqN1vN2Vf4tXhekZfrHhYtr8GbFPL8drdgD4QftfSFWqxTsmBskxKBxcytmaLSQjQzcPEs5xUbjR1r4Ym5KoUUQKKYKNLFZMVLzzm2yqcmW6YwXGA7NEjZ41Fz9eG6Q7bJxxhXrZXshXpfXbWf6i1fRssNgJfuBLYa43Px8HXfc3CvvQcXAtixLYqHcKfeSiZVt2exXM97LPcSeNHhzEj242aVbcspL4GJUvVmGFGoqfZcYD39YQv8sVVrehGpsAqmSjQnerkG7"
    dataPointAddress shouldEqual "jL2aaqw6XU61SZznxeykLpREPzSmZv8bwbjEsJD6DMfXQLgBc12wMmPpVD81JnMAQBBBB9FruzhMpRbC9Gb5jD2vijgUKo8ZxWXhC3ykRnqBEveS3kqT8TXSQZsu1ReSFazVzXdR2BFLedefWDoiFSpnUVsthKboDG82gscdPij8ZMQzwUcMtNaaFiMoBoosspL7vZ1Y8FweVzQkPkcUU37j81tEksjfzBLYbcSsasLXJTPeo3MM7ZqaT7UGK"
    poolDepositAddress shouldEqual "zLSQDVBaERY5Yozrqu1qAjKj8vrFii6dLae6caogo8m52VBcWySSZPMApEF6xWng6b2KYPMCtGLxqGN8JnGHeczJcoC9eEKqWafCJbzfn8NNgfV8GcZZNjvgi3Nim69ggxfVpCHAz5q3mqq3yZwvprZf3XcZLRYeMTRrYP6eFgeEBsKokyPXoaH6wTGAiyh3LmTjNCagH9NaHyJg7nnKMxkioD9U16AKo89C5iH9hWEdvfhdJb"
    Blake2b256(epochPrepErgoTree.bytes).encodeHex shouldEqual "443888412810a47e5963c8a6e2efc3ba8d159a2e09f7aab43ce5117b12a45fae"
    Blake2b256(liveEpochErgoTree.bytes).encodeHex  shouldEqual "fe15e68ca1d4523a520e7bb9f14cf914a026bdd3406fce206ee2a4ce03dd4ff7"
    minPoolBoxValue shouldEqual 44000000
  }
}
