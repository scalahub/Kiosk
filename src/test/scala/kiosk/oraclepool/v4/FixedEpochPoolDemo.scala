package kiosk.oraclepool.v4

import kiosk.ergo._
import org.scalatest.{Matchers, PropSpec}
import scorex.crypto.hash.Blake2b256

class FixedEpochPoolDemo extends PropSpec with FixedEpochPool with Matchers {
  lazy val minBoxValue = 2000000 // min value to remain AFTER paying rewards. For min value to keep BEFORE rewards, see minPoolBoxValue
  override def livePeriod = 20 // blocks
  override def prepPeriod = 10 // blocks
  override def buffer = 4 // blocks
  override def maxDeviation: Int = 50 // percent
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

    liveEpochAddress shouldEqual "721dZvzcdzwTnHPjXqmmAuHbCv2xLdV5DnV3v3N9pkeCdxfGWafCHhd2akBvbw9L8MaADc25WddsAfuq3xbxz9s2xMQZLou4srgWs9GGynetE6JDphEAJWcuZo7VDXxkJKJnWBk2nTDKaqRk6v2DeYtVN97RaTX1P4oJu1GogEVfejGDVJ5NvtiAx4k2zKErMtWBdJ3G591KUF5gsdfg3Xuum9zv2Mb2YXw4EYjy8qmuTZedspL8HM86PC6NNNv2Dq5RLkwSBBcvEa9yC3jz8pCueDuLTUkhgKw3xpfomWzRqiFsTj5w1J3utDoLktrb68Bc1gWgkDbxedGLSpNiRnC6w1nCzw3eJkc5RjcCe5MHVHGQm6TAkFt6rNG57mckvQ7VwmqrEwNsELzY416sFNJsxMzuaSqfGvQHupFqZqKY32D6UmjLA5zRaNhKK5GKuydZuByhVuDcD5kcfy7osqKLDTxgva8iM8jouHdsxFYDHCL5i3KACSrNjHT2kG6qF7ybVJp4Dnau7z4fiNWVvRqB1WuTebH"
    epochPrepAddress shouldEqual "Gxd4hMRT5aE7dxAoMphWPru1PKo8djzsVRLiB2kQ9VkWrFSdo7NPTL1xjunxhvNfSAoumPrHRaabe6FkH83qsgVPhEb231egb8kHCkbLntA4L72KibkQgkpiPmP1VSyFtcReEJPHj8dx1oxMFANXr8jyc3wbJRHhkHCYwCcMm7Djfe5ZRSSoiRqxEx94NnDPBfS7YJNTazBhJPX3eRQnQidYVABqd2kwV45qRnamCUk61vag1tA6D3UL8vFjafvxwneJLmExtwqgJKQkUHRgch4BJ1fuqAvCLapesp614g1P5iQqCaDAbjTRkGPbL1oqPKNNFQgANN1dGLzRdPceUBgJEeWe6vswukWAE8FCDbQJQvMbQ9bXLWHKEHom1KYP6MZXe7pWfTrfYN"
    dataPointAddress shouldEqual "jL2aaqw6XU61SZznxeykLpREPzSmZv8bwbjEsJD6DMfXQLgBc12wMmPpVD81JnLhtqhKuMLVRHVA2SeTxCETX5oBPF8VJLW6DvqrhwZd5SDUHdVKb6Xuwxww3A1UD6ZhgjnfC3vWT2Jq7wpUWA3E5soQ59T8sQFe4VbYC7CbyQ9jNqw64YtL98VQFLod4WNHEko8E6zhsEhHgFAc2NzjtiDBdWAWDHvM3y32UwiJqJdg8PcgLqDqNTv7hg58v"
    poolDepositAddress shouldEqual "zLSQDVBaGnzeu2uR1xCCCC5kmr8CW6CnzKTDNQfg3kWmwUKthHgJWHGTcKn9vrasRhvM4V5jgbGRE5xh9ZJsMAETmbQh2p4W3ks28PJYSAjyyLYge7uqbdsnwGuWBkzZDswnBPjt9hifCpX4sVcqhrGwub19c4FQYLMMg2fKJLTM35MDVNmT7jQESMg3bGT3QHiXT4sgawH9z21Mjt11N9c2ZzZQPFJixYv2LNoE9Z152vmFVG"
    Blake2b256(epochPrepErgoTree.bytes).encodeHex shouldEqual "8b73c05ccd70521abcc194aac67575b080ea64d70e451e9e900b0582da886242"
    Blake2b256(liveEpochErgoTree.bytes).encodeHex  shouldEqual "116e21360f20c97f3d80a89232b53e0a70f9555ef4e4eeea41af34bf8071c4ff"
    minPoolBoxValue shouldEqual 44000000
  }
}
