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

    liveEpochAddress shouldEqual "721dZvzcdzwTnHPjXqmmAuHbCv2xLdV5DnV3v3N9pkeCdxfGWafCHhd2akBvbw9L8MaADc25WddsAfuq3xbxz9s2xMQZLou4srgWpUVhPUP96Ks7HQJct2TEcc8tkSL9depYhGzdvsvKCtgTpgQSB2DazgnCsSyoK3E1JFcwEXq8qvGGKBHQYH7MenZfH6rNn66FHAseKrw4HgGnc8TzMmvirptb5dQ9hDoWEADrWbQ1ma4GrgPwb2Yrso4uuV9rgh3b3zWV6QoF2GFFyk4i7koPwFyWS2cWs2raP6gWa2C1c6vs5TxeLV7JXdLXGTZZ2Sck3XAH2fbMQNZdP4KAFnAG14GPUzb5Gjt2qqBKSycdvpdN4b5fuipCbM7BW1WUERnDAbduwYcY57LJXvRWB7JQiVAEreU3qP2YDKoXK5vVX8UhkFP9N9igEQkmViomfkMjRfKBygMaWtAFsLmiaP3cr85WNdbwA9gWfwRNEnaR9Hwic5U7aBSvYFUGKmnwUYTqHmLRjE6PvSpZKfmc77GDqBHUs84"
    epochPrepAddress shouldEqual "Gxd4hMRT5aE7dxAoMpimyk6hBPUyAWHPhBbYwmidodjige5z5jAtFBqbSXhrqf4B9o3JXCPxikLd7iTXkFeDKBc6USahTCx2qiovBRE289wFyB9Vc2aF3e1to5BCwX62db5ZKCMwwWGZKMpjV715D5CDXix6VTtQGz7vpuPfdTv5MhNrjeoF346a3MTRuu4thc569u7P9CaPhpYpyoGEk78sGTJD84R1nc8uSPDEEnuJkqtBfrS333ccV1gk89wgDK3YiidjG4NcXvXZ41ttYDhAdsoWf7nE8sxRw1TCDKqJmbmcnTysKuih2b31x8JRmb42p18c8h4H3suzsz3DQ94yWu19MeTGEiL2rqMGeicjuhpWKhh8sY1xPYrYPBmj5eLqKm2jiJCwho"
    dataPointAddress shouldEqual "jL2aaqw6XU61SZznxeykLpREPzSmZv8bwbjEsJD6DMfXQLgBc12wMmPpVD81JnLkTgyru5UkV8yiMN6gmn74AEfrCcbXTfpqSEDQC9CQBTsdPpjhG4irmABXvCupfziod4bJgL7TExbWHcg1hzwQ4eaHQsKNpGBcsvGvcx2vWYaMfgxFBL9zW6Z2p74aEL69g9DPG8bhynWrkhn8ZSk6KZyZmAgnVJabySmRtYgW9karT9Ri2YhkmxLwDjTDB"
    poolDepositAddress shouldEqual "zLSQDVBaCBPKtwWCSMmumApRHdA3TWESxhseSMvQPwmL8QaAn9XXrZgmwgJsyrKF4tEThoWJNE8q2CXpE3jWLo8tZvv4tt7kUcYLS4p3LK7po1Qjg7EEdvVMKzHT5Ymgix8UnD3WMHZ9Jmo6h3kubzvmX2MqaQJXcv3oQ7ijeEmCnmGdmv7iPqt5Ni5LNvEsVt6C3JrGZFxVDvEsLiDc9kGJkXcMYnJ14VDawX4LpAkQJBrDEk"
    Blake2b256(epochPrepErgoTree.bytes).encodeHex shouldEqual "00c6b2a9e9185c93469ba2e8745abd29ab003a9c365d5b4486943ed0e57a7579"
    Blake2b256(liveEpochErgoTree.bytes).encodeHex  shouldEqual "28580afbd08f7170bd1534ae42a9ab89f2b229dde5026762dc799890c7f9143e"
    minPoolBoxValue shouldEqual 44000000
  }
}
