package kiosk.oraclepool.v3

import kiosk.ergo._
import org.scalatest.{Matchers, PropSpec}
import scorex.crypto.hash.Blake2b256

class OraclePoolDemo extends PropSpec with OraclePool with Matchers {
  lazy val minBoxValue = 2000000 // min value to remain AFTER paying rewards. For min value to keep BEFORE rewards, see minPoolBoxValue
  override def livePeriod = 20 // blocks
  override def prepPeriod = 10 // blocks
  override def buffer = 4 // blocks
  override def errorMargin: Int = 50 // percent

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
    println("LiveEpochScriptHash: (for hard-coding in Datapoint script) " + Blake2b256(liveEpochErgoTree.bytes).encodeHex)
    println("Min box value: " + minPoolBoxValue)

    liveEpochAddress shouldEqual "USNVj4rN2DctyA1X4Wc7k1DpgW8MgSmLbXxeDgL3CvheUhjTRZPB4Pp6tqG8yZnLQkJVEvgHeWQjMwbKcYdKkGwKYN7coUCPXDwVjsniPEu7wuyzVsWrDH4q8CRatjfiVE3U8growjbfNUq6xcg8AQdShGAhduZpYeUULp7bgTHwQe7c1oWaFLKszSaZwKExY8TtrvJJacK4mj5bMFAzYGwrTNvMenpYqaiUfPd5e5i2vx3dT23RXYpJs6GZ4Mgyr2yYo98MKWUhxnfBK4UBSm1MLwH3p3oFii3L2KkUrZpBtP5tckGHVi98Hwew2cMhWNugyVRv328MSXTm8USZx1DpAvRxDd4JgCjnxzfWyFUa1qvWXKDcpig9Q9WMucXn8USd1vjT5n5V4h5kBxqsNFZzRmbTGA7KUmgnTu7kFE5PjkfJZSPNZJNmazG6UmjBZhs6DXnaHTnccFtjC11eWYsMm1pU4d4Y4fsJpd6z"
    epochPrepAddress shouldEqual "Gxd4hMRT5aE7dxAoMpoGtj74HorsenAQqMhxvzpT7GQEbeUTrx3ZjYHotQn54Ye1ddrLLmLsya6ryMvJYPkcdkCvyv6CMBshCDpEebRdfKGxKaNZ3QEYPomT1eqX594zZJhXddD9eGSXndCzSWLVkcxpFx3ubCWo7zCox1hZqWMbSUGmXLJPWRLpGb8DTBTzxEfqSugnJEUwgg7a2hyh61wthK4FCM7y3zk4vNYYrdGWwKAW2Dz5VPoHaMh3zbRtQbwpdfYBdSbbBHn4MexaXe9SHNU9aP5mSzb8cnYGgE22kLgtYeBq3BPmqeJp3usRz3QYuCE8Z727n6fFGHzJw5drVWetG24eqYkzVoN7mF6DtRxsjrXnQF3u3ofzgzKPgR7Hi6Me8Puz2s"
    dataPointAddress shouldEqual "jL2aaqw6XU61SZznxeykLpREPzSmZv8bwbjEsJD6DMfXQLgBc12wMmPpVD81JnLuZUjHRKPysKxKQhcBaqDs7ZAtYwRuYmQojzKK9bHXDUY8N4BiJx8AUG8VEaggD4ztWSeQHrW7EbFxpXgaMKuzuN1Gq4zoYDArstgcrHKwg2uCeGeXiydQXRWEyE8e6noAP13nUBSmNNNVqkM9JGUVAJYo4GGdVFg8FRtFWcNdtbxCKfw4JGVhakCGj4qvd"
    poolDepositAddress shouldEqual "zLSQDVBaDkFiQhpgVYqu9saX3ppCMzmma1qdryGH1x1GTkAjU9vVodDsYrk3H5UvqDmdxJLoDADg69KXyL9gVGW2NER7GxMotdh46Bzr9P9tJwPdgvNhSdoXYrLTemKadCU46aGy81YneoKB7xjz3a1v4Aar3n71XysQ6HwdKcJt8WFKqbZmRx4JnJTtBUtsdD184oU623BXA93cGrG1fFuFzSALqGztnS9Ai4JP6NcM8LE2wU"
    Blake2b256(epochPrepErgoTree.bytes).encodeHex shouldEqual "2fdd5bb4546e99a50d0e19b0d16ad7bcec80b8c797927aaf18c6ec643d9b3f16"
    Blake2b256(liveEpochErgoTree.bytes).encodeHex shouldEqual "79974b2314c531e62776e6bc4babff35b37b178cebf0976fc0f416ff34ddbc4f"
    minPoolBoxValue shouldEqual 44000000
  }
}
