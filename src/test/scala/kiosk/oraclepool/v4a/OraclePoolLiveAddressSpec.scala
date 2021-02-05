package kiosk.oraclepool.v4a

import kiosk.ergo._
import org.scalatest.{Matchers, PropSpec}
import scorex.crypto.hash.Blake2b256

class OraclePoolLiveAddressSpec extends PropSpec with Matchers {
  lazy val minBoxValue = 2000000 // min value to remain AFTER paying rewards. For min value to keep BEFORE rewards, see minPoolBoxValue
  val fixedEpochPoolLive = new OraclePoolLive {}

  import fixedEpochPoolLive._

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

    liveEpochAddress shouldEqual "8D6pdYVRvF6PcDZZ8HXaEv1VyWmYC8VV5mfmuVPCBHLCzzjWUu6vvrpJvN8uT7rJMKzJbGmVcaDJirPFeuTV1jgk3E1sF8KYKaQjvohyXfPsZhCqhsQ8oh5c3EKbmC4DeMBBxaEzUZZFmvFwpsoGMJvhwQKpwuEQZruTwdzjUxwfFMKyNtTBbAr3Pmz6NeB5ewyj2bSFcfN791G2jKB3VENA7pncaX3bPhL6aEDJstFBELa9a7uTeevA1sjcUwTyUDfShwyM2hE1RxRrCFMfiuzcR3nJYVLEsn3Y1ge6ThjQg8H3xFpXsJ6SfXtFhX2PdnhyjMD9mtkCjM6icsqNp12R7J58HenC1fv8KpuBU7GEmQAHEFr28Gr8GKafhiTsXzjtqbiV9b78A4wtWUBgbEPfrKhV1eB3fVKPQ73Gw8xwxTLBbXLKyVHT9kcrnUPQBGij4H1oaeNz2cr7eMdwiP2aA4XjUHDnct7ZiHn243UnqP7gveBGf1fcZMYNkjiXhFCRsjMhBQtjE9gtBMNoTJd3Y9tdjYn1YcHWbKQLvj27eD4XXpSSEXodBFuhM"
    epochPrepAddress shouldEqual "2DSWBK78v9sqbTLKupuf7tmXwVdNr6jPZPjxvaNxgCRkYMPvWagyAu8qo3Uq8Df1AdWK12B88MFEnd71bFiAA1y9hycNziw99PgyaaLiWhdHUDfTziUoZTLbQWTEnaAeqcwzodotewJxQpGEW4Hx5DBkqQgrYvSvXGqHdhZHLztee3Sa8SMKEsRuTEhrR6jCYvQbwYfLG7sPfXoBGWfARLwmjSm2UkzJTYXwNEvqmr89KAVn3U3UFsxbabLoe56oddeRfpNSMJvuzf8UzjiYt7ir2uBPAJMm8qRxtxAu2Ka6XdFTUtbyE7NEeLtXFvkv2RoorZfzMNkDixt5PHNM8WZiq2j1kcsAtcR5ctzB8bw62ATkR6JVa97D6GHTM7XZTgY5X4dnEuTg4PWm"
    dataPointAddress shouldEqual "jL2aaqw6XU61SZznvnw681zGfmUQkZouLZ8uvwy36MJRbqZRxr7sFbLJKMDEFy41GXWBChWqa6tRDst9xPdPsq1T1UTgYqLBCexE99Vgc68b3TjV6ai5yNtv1dXwJrz5iAqCKcagzTf8ZSA2Nt2hAMLCNFyRxLaDbYc3Wj4CuL1hvgYEPwnCJfRURkNReWa8zrPBY212EynDKGa3RdxWesA4AwaKtYwaaNjwdCaQ5bvScjw45mgKEJDmq8F1x"
    poolDepositAddress shouldEqual "zLSQDVBaHb3fokB31Fv5jdpRdGAbnkNgsFas3BrADM1XBFqfZjGNAh7K94gRRdyjcRtq6B9tGKRkThczZR5vtYzAD5TTdML2bpBmpdZqkGVb6kSj1wH8JEiqx1L54GcU9CtFRdN64B1sccA41DR3t1uYJh3BBCDcBiZyVFqMF3MenohN5gt8JTMcUzwzDjCUbtmt2MULpUDZvNPgnwFmFQTAqEHKEkunaKZPHs8WsnMJzhXHbU"
    Blake2b256(epochPrepErgoTree.bytes).encodeHex shouldEqual "a35103a7116ccce5c1abdb1fe371e06e73c3b0b616d178d9be643cc5b1f8f36e"
    Blake2b256(liveEpochErgoTree.bytes).encodeHex shouldEqual "31290f347f33d70dd573661b744b19cbe431f581802b8920e85192e2b6eecb1f"
    minPoolBoxValue shouldEqual 222000000
  }
}
