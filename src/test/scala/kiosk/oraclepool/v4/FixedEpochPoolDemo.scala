package kiosk.oraclepool.v4

import kiosk.ergo._
import org.scalatest.{Matchers, PropSpec}
import scorex.crypto.hash.Blake2b256

class FixedEpochPoolDemo extends PropSpec with FixedEpochPool with Matchers {
  lazy val minBoxValue = 2000000 // min value to remain AFTER paying rewards. For min value to keep BEFORE rewards, see minPoolBoxValue
  override def livePeriod = 20 // blocks
  override def prepPeriod = 10 // blocks
  override def buffer = 4 // blocks
  override def maxDeviation: Int = 1 // percent
  override def maxGlobalDeviation: Int = 5 // percent
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

    liveEpochAddress shouldEqual "4vt3UT1HYdnRAepr8FJfPoi3ZwyfH3W2UB4VsXMTkvPuGnzGFGTbz1sdQoDYuiC9zSwKoED14g1yhAR3XK9ohCi1ej7UjLHJHZvJf5pRk3um8VhSYYHizRnrZnQL349UbYSpSAYywz6S6hJ3vbFrnGvoVQvJMn5tnXCrBghscKcTpDRKZnfY2XKLT3k2rqup83LQ5i73X4cx2oNWmtEtUU168AWY7PmuZizPbi9XM8sdLbd4i9DGv5mSRQJtYfAB6BAoBCEBi6dwzJxGVgtgC1cRew16wnHk23Pdu1yCRhgxXwiQSEerMCe4iW26Z9FQTYBsywSBtspd5WZ1QF4bHscvJcDFfYG1QhufwA27Zxyb7oGp1saeuLwbFrGxdHgtnpJUKmUyXd7EUVys4zAQoS7tL3cqQKeD6sQkDfw8AfwcUPDTfNzSXByD69smGcXzxDNUUqNrYgV24DPLaJdLhdo63TesUfqExawQWJbAXbdJJPqxRNxFvR6hh316fbMEz1NYtvRMw9795bojDQ9TveE2BSf3t719U9dctqa7wmtRXoWw4ruj88AEHhkzxugc9U9MnFL5XU6RJYGxRYE"
    epochPrepAddress shouldEqual "Gxd4hMRT5aE7dxAoMptnVVphtav6H7W8fJP5Bheas71p9Y9V9rUy9kWNFZEW7xDJCBhtp3tcH3dfR2bidnXzXhcZ9VeDjHJxBYyeGmSgn4GE1YZXonAZv1B5ALbEE1VJ93f1zZjrs1mjLk9ymjUn7bFMKGosZSUJA2d7NWRJEdbBANo8Bw5YcSybfcGDugW2khq4gfxmwuSJrJf1KyzRbE5DWi3F9yuxGS5Z4XFhEKravopC1Tf2CPVRtFxtu3uUY1KwCiJr5XsDHPjhXJMwqsSaLabTyadi9fP48diiMUwgY72tB7QJkh129ZZzk1fBN5Yt4G6U3U2XPxd6pyYUy4rJXyNnmPn7qmUNUfy6piPJqoWjv7Y8Xky7A55dBixLKLPxggbGq5aF11"
    dataPointAddress shouldEqual "jL2aaqw6XU61SZznxeykLpREPzSmZv8bwbjEsJD6DMfXQLgBc12wMmPpVD81JnM6hyq6XK3yCwqUDbKhDsrtAhroZHDvAdGmiYuhzxVfMk7TatirRMRuxtJd7Uw1SxD9rPpoJNEi5Bqy434u6f9MnYWmAjQsGVm9uoKQsjjhfhaNLq8MeFqmV2TMVDg8pWDkK16XeoJs2Th6RHMDSd3zVDGLHCZHEPRZe3ptuCTKCxkwrrwzBxwQM4etCRumc"
    poolDepositAddress shouldEqual "zLSQDVBaLejBmXMeb8NUspQGT69PjPi3Uac8B1bhBVzYoxdq6yGcfqrau3hzDZCWBts671hyVGrw2N5xHTjshw41QbuP8dj6pQocjG5JaWhhNjJaGVhyiqLkWpCapLQsUdCLEJF2EAoAGCmcCs5oAA53ykpWoUaptyUFPPtdt9nyJ32AuDCzBUVseWBVCrq8aX4WyQnmkPr4Me6p2Ssiqf74gnhWBH1cV3UKu8xjXVAzdwttXA"
    Blake2b256(epochPrepErgoTree.bytes).encodeHex shouldEqual "ff643d16f36c138c192e3304ef805d1fe9816d96a8f03cea1f029bf9e61c1c09"
    Blake2b256(liveEpochErgoTree.bytes).encodeHex  shouldEqual "dd1cd17855a557db98a3469753f65b49cd38389e7647cf7cbf8bbe111d0b4c2e"
    minPoolBoxValue shouldEqual 44000000
  }
}
