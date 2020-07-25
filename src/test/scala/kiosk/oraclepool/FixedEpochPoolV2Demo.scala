package kiosk.oraclepool

import kiosk.ECC
import kiosk.ergo._
import org.scalatest.{Matchers, PropSpec}
import scorex.crypto.hash.Blake2b256

class FixedEpochPoolV2Demo extends PropSpec with FixedEpochPoolV2 with Matchers {
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
    println("EpochPrepScriptHash: (for R6) " + Blake2b256(KioskErgoTree(epochPrepErgoTree).serialize).encodeHex)

    liveEpochAddress shouldEqual "PuQMaBLq64VcFcVLSW2Ds6Hrab2u3RWUBupkzNa5p1Pi3EXdnUeMyaVf9KGoyhjELW7TEduGZyupFe2KGJX9hMdvkp1JFffM3KtxcNy5jNCDnDNUdtCCrVRthURQGsZMTaG7HqeFxQdBruc24ZAmsiAWDXdaoWX14Gekm6W7mmA8qgozDfdMmhLmX2dpMbYkQ2qyoRrc1UYZY5EeoEYQPT6ZWyr371511xVkKNSpGncFHs2HekuzuqeQchTmnRdXZAXEzAJS9NCtqqmzCVE9PQpSK9Wh7wNSRf9XoNMNVbuAR5JxprddB7Lb9tjYiX8yCAa68wdjCRbJZinu2rxxf8ZtrTTrKzPdWEuZJ22tTzusAJSH8bFwxgj8EPxtdMgYn9bBpMKsG7itgA1YwMD78nJAasXKR6CMEarCCwad5B1PXHqA59EjpzEjB9ZXeM7GSsnG2hPT36"
    epochPrepAddress shouldEqual "2DSXNByubqm7twFq5ERLwAN8ixwJ4mDH1fQt1u535WLkPZTyzBj8yeU6xWaNk6AcvQPNKmgpA3zvwrqMQV8Ra33rkW1EdH7zT8LM6zRBSd7Yj7NPc9Byw263miTR4HM55os7fH5VfiCydbi7KD8YuvhzzgEhELQzvZMWr2wVPn6V2Zix3J4qRqRCEUGzqEUA2rsxKXw2dp79yn3YhPboFQptWzvPs2PTUFpFgUYYyUmZaRMSWLe8iZM8pVY2qCameXH5DbkJgdNc3ptkux7gAFGGHZmHEWerqY8SmKvZki77bFDAwcyms6yssnCKnpoXCoKCpqwybqr8ERMTeHKgyANx5TM94fuBAfvzY6wNqEw4ccW3aydcs5nGPxWdfNhJz5gYMHnT217JGDEG"
    dataPointAddress shouldEqual "jL2aaqw6XU61SZznxeykLpREPzSmZv8bwbjEsJD6DMfXQLgBc12wMmPpVD81JnLqDbKpKmGc64uDYBJET2GCZ69gjjWMQWqq7ySzbaHYEs1vCZuS59HsPXiXCMfd9TkeFqWwPEYV5PoqM3fFVKcfS7NcCrj2roU3o4EwWRwgvxLHwxQgVTYhvGD8QZYRi3gwbDri13spWcWjfi2QRwZ6HkC3CohTNJtePy5bzVuzW7R42bf6qaK3ofKy4siir"
    poolDepositAddress shouldEqual "zLSQDVBaDohRCgiUiYJSFwHxH5x52hXnrgK24fKfmHwpRHk1cyRGLGgHYjmg4PEeMuWGw8YE47PTHzoppqQFGFBEobJj33pN9c6MVzirWYjkMHZqGYaZLGyP1h12JVBksjh9vJfZVrBSXBJpZFKKS7QvZ74j48omD4GwsnTHm6onM4YskN5QHVTxrJMby1cz7PRv44g29HckTJ2JCpiTHC6vwoJVwjzXwZgKrhw8CpSxS9r7mi"
    Blake2b256(epochPrepErgoTree.bytes).encodeHex shouldEqual "31a620d9aa2d7253e21e86fad4f14c901db35875ad67dce88df2f627321ef799"
  }
}
