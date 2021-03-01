package kiosk.oraclepool.v6

import kiosk.ergo._
import org.scalatest.{Matchers, PropSpec}
import scorex.crypto.hash.Blake2b256

class OraclePoolLiveAddressSpec extends PropSpec with Matchers {
  lazy val minBoxValue = 2000000 // min value to remain AFTER paying rewards. For min value to keep BEFORE rewards, see minPoolBoxValue
  val epochPoolLive = new OraclePoolLive {}

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

    liveEpochAddress shouldEqual "8D6pdYVRvF6PeiuSWVBR4x2RqKKCjPPqMyTpmRho6EpfWMqnwN4uedKYYTkniyKPEjnjHXpLFLE8a7uszfA7VmsJSHiQJoUcvi8zEWMtrZJ12JHFB78BMYka7SLbXTiNcan4bQnSRRShUcrwMiS4YEBWw5Sc9ZnA5sA9RdXaBNa4HZEcQsY6uQ9PFyrM41bN7gvrqneubdP9ohk2Z9b6g4XqayfnaxXVdGtqWg4kfc8jKU1n6uMAT1MdyR6xb1MJ8StWJCquEonZBFpxBJzkfoQTGqmcdSfw2rCZqJYmqD4HQnyScgLCFsZaXQtfTstWAj1r9LhQCyK7TfL6nZgCWhCW2LYqAoSoUnxpQ5ZBYi1ui4CVPCARVqQds4uvVUTQyY7F6n1jgpCWg3N83soKDS4S964u45egscfN41Fwv3RauSJvVheFoNUXjxDiWihss8jgQxdEcqpz8QM4RLmmN6VGEo4t2pcdVPgkeWNRoEkBX2UeBWag8ZnA3YouyBKCLfCsRe23DLwfgTwEZDpgxeRkytUfY4tMg5NKuMj1KaU7i6d2dWzgJkNTzm95o"
    epochPrepAddress shouldEqual "2Qgfhy6m7AhxRU3xMHBq1XZjv8NnJ2CCW7GupJbKhDNzVR7mtxKCf9kZJP23U8q1bYkEFb7VeNjrFYAo7fbdP3RFWDdTSdeyDWa31ubjFgFUPk2W4Ywjr3ABd15JkpeSRspo1kGQdF7KZQ5hKKG8EViVTyBwvmvKzgtMDQ9oStn67KjRCgcr5PVXpsJ9j54zaWTT7MYiJqkhtzCcM2mfDLQAqBQ4ZsYY5rSYmq3HD3gGnKBTYMvu4BphxP5oPkxE7EHj2ZPKKYrLVSgHbDHqeWbiE5FjfS74v2SMor5MnsK7PZwynCKg4QhfosPHxRvpy7TduzzByjSn5WK187nwcoDq53DH4mN4LJDv9WwFs5iA8Er7shMpeXJ2WDboFmKYxG1q7n6tZWakn77FtPWAxgaUc7cDpqVMwbuJYZCsipKT13nHxqv9vxazJ6gfo9SeQSeyfkf6zSDHdh1pnpwjDdKeu8StUfR4Akw5pn"
    dataPointAddress shouldEqual "jL2aaqw6XU61SZznwZeLEryve79s26LKMtJDCG5AMu3jQ8oUxQ8wREoWLPepqTWZj9hsDWUB3rbGyoCrBTp7j79y5x8BwmjCoEGS7i2LU1SN2b13Rj1RBiN6SJFur9FXcvUH9135m97SAbKHgW54AuX9GMLQTrydRHiuRiKXWDaQM3Wkkdx5EfPob2oKzzR2x63H2PcHRtfQ3mCKdr6K4xoCwm98tSQ9cTSHaUysQVJGn55ojRbbZ5h5tPaoJ"
    poolDepositAddress shouldEqual "zLSQDVBaDycK1yZD45WiF23rJHwsmqkhzmP81isgCxuKV3Hw5d6KgpdmPtizxoT3Abrod2FxT4BVmntPLVjZo2NQaua7wd4rB7XibS3RXBUXCQ4Pic53Ya2kgfzaaLAo7DxXTYJUoUzYWLzn3Gquh788Aof9hqZjgxPJo1fWcsF2i7tjbVMa2MaHbqp3CRL9KdLXe64cjU3U15bGzCxgsfT3StdYsG86dt4hWdZsSPBEmXKAKs"
    updateAddress shouldEqual "9vQZrzvghythEub2uuGhjKFGrPeC8cmxzRsngeojKT1zji7TjQiGBhkNkXbRbMKTt4aidvMYQifLiJXdoXKRqPHwjzcmU2KEMjnY2jBzb5xPHqqSMm1fgV5BK1sdjw67U8nMygZtyKfr6LNGCxSLQZ7ythDoaMWjgKZZLh5wHhgdxJWrn7oPmRTVf3z3ogjd78omiXPmhfLoC6yUajmCn1qhjk197YCHocXaUAvEvutHf9jiFQPWBxa5frry6eKSRVoWMwngaqwgrzTMyH9b7qAnCkMDxzYeCj2c7q1FCoH9vTwra8c5NiKrnEcWaP8AFwLxnoZMF9rogj6RVBTSXvKCsZiGZCvE9bT9rYvug5uyo2fuXkxPvGVGGkQa7buDCMonihZUeK2KTFyBtuvFTodCJXLkkxVLcxRorfpK7gNnp1St93iJh5Dg8hkjCeEvqMXb5KQ1VGP2m9sYjws6C9NbZvTNKFkohLv5ZwexUSU2KbAs2zQDytv9FwyZRgP5uSphBzoBWDiX1QEwv18ryFovCqTq4pU31or"
    Blake2b256(epochPrepErgoTree.bytes).encodeHex shouldEqual "36c90ca2d7aeaa765caa2b9c218ba333bc94553dfcc362983ab81298e0128b80"
    Blake2b256(liveEpochErgoTree.bytes).encodeHex shouldEqual "8d0ef72cba2bdaf92cd6cea6bdf2cc5c5537ad7129271d67de404448576935d8"
    minPoolBoxValue shouldEqual 132000000
  }
}
