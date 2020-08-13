package kiosk.oraclepool.v3

import kiosk.ergo._
import org.scalatest.{Matchers, PropSpec}
import scorex.crypto.hash.Blake2b256

class FixedEpochPoolDemo extends PropSpec with FixedEpochPool with Matchers {
  val minBoxValue = 2000000
  override lazy val livePeriod = 5 // blocks
  override lazy val prepPeriod = 5 // blocks
  override lazy val buffer = 4 // blocks
  override lazy val errorMargin: Int = 10 // percent

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
    println("EpochPrepScriptHash: (for R6 of LiveEpochBox) " + Blake2b256(KioskErgoTree(epochPrepErgoTree).serialize).encodeHex)
    println("LiveEpochScriptHash: (for hard-coding in Datapoint script) "+Blake2b256(liveEpochErgoTree.bytes).encodeHex)
    println("Min box value: "+minPoolBoxValue)

    liveEpochAddress shouldEqual "USNVj4rN2DcqBDatgFLgvYuht7oUZZPMHV9ktyme2Hruikxmow5ma8fiwXYDNhfoMDecWirdoe7Kx2LLWnRaV1jM3XwSkAVe9jpd558uXemCpwfdGXhvpM5nQsmns8fn5wYEjNTVmL5bhhCYHeTnVGPh2TLV6W8WtDbo91EreueTWRDXKpBtRPU8PAdy7rDg8MrHET7TxTNZSMTGdgLeknSEHute5k5cDsTSG7CTUcJotiVt3uN9vczuef94HEMjzsLPzV4x9x18M2nXYP676dVQkPkU1skYpPAZQpogQxMg7pF56uzXD4upGUomzJn5vd6f7mBWhuNwTt8DTEiKpgw4vLJdcn4oPa6CN7N3jXGpeUwAKreZvVhjdExNgTNWk5MWbDZmzo3DEapymtufJG9bgr9MrEfT6NbQzktDMv9FifBj7Zf8BCPshprgDjx6f7LrsbzyECFqQaQctqMJY8DdWfZqHhKAKzbTrwmh"
    epochPrepAddress shouldEqual "Gxd4hMRT6J1SA6D3tfpzT4XSFduLqNYjSQfH3vB46ZcpRSbV3dVPQ18yHPVQAncDunnz5bzUGBrzBJM5EqipR6LZx8DH5NtEMKoL1jmdz4mC4N3r9ptXvn1Xu7bNMyyjMSdN8L3wYwBPSvcUKVriRYvWdcwiFYXvj51u597NmFEi4TPTdfYWu4cs5awzNqFExWZd5Xws8wBTXUD6zhwsPF2Z9HnhDUCgWrqSugY8k7vMRBr1cxRaEU2HRUgh1fQjvAvGZd3UfamFcWe1z1jqsLLUmb2m1MLCyGY19gmTMwVTT5BYmN44vvBSoyij8eNoYguBxjXdpJiTxWCid67korqBMRmHUaxPfZUBD6oz9weS9pDNwQJyUEPQ9at83cR8ATxvJLdvZStDu5"
    dataPointAddress shouldEqual "jL2aaqw6XU61SZznxeykLpREPzSmZv8bwbjEsJD6DMfXQLgBc12wMmPpVD81JnLkZVC5wrdJX1qGjVTip2avhM9gADhDmMru6t5XAkb3Dv73Z9ZqP6zm96MhBZRvMUhzreNHMGFUwbzgzFeVCsLN8RRDyCzYEJb5NdX1639ofgrY12auU37nzJEkJAY5pMGgvoqBQi3QiX33vVAAAkfiQNXsmXDnMGKbD4RnZkr31zXQeNmzSoXimmhHDUnPv"
    poolDepositAddress shouldEqual "zLSQDVBaGXZTutz9snE4UBbRzmx7Juaj4R2KCsSVKhQqaupCuk8vz8PxRSjghHSNsd893iMEqoug6r8GHDZ4PLxVckksiG6hgDQPt8tu68EokDqJkUp9iRjSWszv1JFigeHe2fVvA9Uy229zWsv643UpYXjQEJkKdCZvthDLTn26aaR9arYSLGM2aTWGwrXmB2bHVRiPCjYn5mJwS7LssvJ9ycEARV5dXe4scDWRB7sUi234XN"
    Blake2b256(epochPrepErgoTree.bytes).encodeHex shouldEqual "83743c43c5b96d99996395bfeaa28a203f784dd1b73ab278e6ca44e430607da8"
    Blake2b256(liveEpochErgoTree.bytes).encodeHex  shouldEqual "293c7b7e0c55bcd8650bd6eb551144f2480f11292cd5a76bd00a65fe062ca5e6"
    minPoolBoxValue shouldEqual 10000000
  }
}
