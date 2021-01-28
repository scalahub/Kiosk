package kiosk.oraclepool.v5

import kiosk.ergo._
import org.scalatest.{Matchers, PropSpec}
import scorex.crypto.hash.Blake2b256

class EpochPoolLiveAddressSpec extends PropSpec with Matchers {
  lazy val minBoxValue = 2000000 // min value to remain AFTER paying rewards. For min value to keep BEFORE rewards, see minPoolBoxValue
  val epochPoolLive = new EpochPoolLive {}
  import epochPoolLive._
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
    println("updateAddress: " + updateAddress)
    println("EpochPrepScriptHash: (for R6 of LiveEpochBox) " + Blake2b256(KioskErgoTree(epochPrepErgoTree).serialize).encodeHex)
    println("LiveEpochScriptHash: (for hard-coding in Datapoint script) " + Blake2b256(liveEpochErgoTree.bytes).encodeHex)
    println("Min box value: " + minPoolBoxValue)

    liveEpochAddress shouldEqual "8D6pdYVRvF6PcDZZ8HXaEv1VyWmYC8VV5mfmuVPCBHLCzzjWUu6vvrpJvN8uT7rJMKzJbGmVcaDJirPFeuTWNpS2Lb8MVQmgU7dXfe2pwwroAWmaKWRK1L9ZkoKEu5jhMqEKS1fKhfAUZAqZtocUpZqqrmUxKgjTbdUzCwkpSJZucV9MxiXuj2Z2FT8Jo7Bd2CMEYLCXKfTwn7U5TGBvLxD8eAPu3VksTuvXQtUABQj4GGcQNDf5qWBbrfLY7rvSy1PojUA3jxX9aiALd7cd8XWwBLUC9MHyx1wcw5BHVZW6K4GasUUg2ywdBJVkHuZXbpi5Rm9oywWjVihDmgX4vYtXqXt1NhwXk5epAKZMorM5NeD5qmgiagu2dQuFFtr7PaMLGfU2RmCmS6cqUgJKPx5nvgZq8K4fGkkoKmi9CbMnriknuE9TEZPZavcL3fam3Tiaz9L3nGpu6XotE9M3G8vvF5zmqePXQ9qMXscxKv9LssH6b8LVotgDJVS2zf2XEbQ9eNiV2xhNy65iB6nZvoibBfGhKxXi5gqCJxesT58F5LsgEtofiSVHE5579"
    epochPrepAddress shouldEqual "7DY5qpcRgHc59umTpZQT9aTi34QNxrqfryKaawE9HLCN9YwUTmuKKDqPvejg6XXgnBcCtjMaP6mMCdmmZMcv1eWinB4bqBAMAdcdqQMsy8XKsJggmncRSG5urLANw5njNjgxaFB1oENtyJ8fGr3A3dpfXsMMnmr4iGq1BQvJf67qv6gYQQBhAEjVBjz8CdqMfSLZPE97cx435R418k61o6SkxBuu4mrpFrY6PypYTAuLQAZuKgELpc8yNvpGLZ8PrrGGHg984AvEPKeiH86W8dgAXocYDVjmSjzHBUXHvXYvP2aYgnty8ikMehWDJQN8JTjEBa5w8Y1JsbPhF6biKYftubzvHxfauVodSh55Vt1fM3H2cnTThWESXUgHgoBjHxUwaqfz1z3EyVgBdGZbUTtCiWyMMgZvs2JieuqiuerTowFwbMGouMLp2q9rcUHnSWkcNkMk63FRFBkAhW1bXEaYj9eMqhrXJLUbnLS"
    dataPointAddress shouldEqual "jL2aaqw6XU61SZznvnw681zGfmUQkZouLZ8uvwy36MJRbqZRxr7sFbLJKMDEFy47cEjqAtaJFdmewp1Q7J5nrUv3Bno686PSF3h4cVuRN2ZTyBPyZimGeTW47sYAiSrQ6dXT2zb1JaZeCjb1VJenWRG1nxkQV6n6PHF446kbWPvmLPPf4XCB71djwsrjQSriUfEPxkUXNDrqnWQWnepLmqs8it5AhUmH1LZpvv4LGCZSZpLS7BgFoHAiGyfuu"
    poolDepositAddress shouldEqual "zLSQDVBaHZidTzuwrLSAQgJzknwrJrhhjkmLfBBdLU2zKb3wGFgkQ6aWeRxSp5ex9R8QXE8KTqnDGQs6RceVV2aRPakYNPa1RNi8XQcUiRepFjVyNYwGKDKTZ8yjMGsp9AP3xeR2roCpVaxo3VfnwM8JQp4fUGELFfFqLmPhCzMmv3MUJzA8sUtnJFY34yZyspcbKiCBt1P6rPxj5JH2Ge1dWm6NviWshJvntZ72GexBGUZkxj"
    updateAddress shouldEqual "6Vs43fLosHJsXYkR8kcqxbbvzBu25EagFq8QCiyBC1WZSpRtEXLMH69e1WM7uAA7HR526t6xZbajsyUA1uXSkgukFfHUYsju5G815ydkAZkwbatyWNoBA8Kuqv7HHR9BqqnmpvvM7modBc13bqHkUS9TfzsShL9Ud4tJ8rZ2HC4u2HT7fDeeSN7gPADUZ9vMPmmHafgzf8fd17HiMC1yTEcJLdPYh4VmPMmAkLrVAoRMzQwM6h53WDbGXno34fCrGmZ2pVnUNZCheTzSJq76E6xUB7g2CyyCjbXC35U195GFpo4PSWWkb32nE2TgSLto48bSbGUAKz4iqMXstNbE1J6a7chJynKkrWz7Bbt3wo4UM3jPeG2GxbdGkXDX3rfh3w3KfqdiHoMWPzgzoRuGx7jaf6WwnpzzHmsitnTNfkbvVDuaxSYFJzpwenxV9e7QmnDj9DPWYSdcVksRtUei1Vb32ycVs2LDuXDj3gfe4S84p6XzbA1QnhtU3XQabdizcDVaZBX3rwYwLnqP"
    Blake2b256(epochPrepErgoTree.bytes).encodeHex shouldEqual "a2a0cde23c2b1f0d8b3b8f3654b87de229f1ebb8e57e5c775771015dbeb2f09a"
    Blake2b256(liveEpochErgoTree.bytes).encodeHex shouldEqual "69c3eaf2246a293e38003e0aded6b90778ddb54f2ae52b2170628bd6a36b2e7c"
    minPoolBoxValue shouldEqual 262000000
  }
}
