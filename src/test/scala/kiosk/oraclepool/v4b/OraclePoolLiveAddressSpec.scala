package kiosk.oraclepool.v4b

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

    liveEpochAddress shouldEqual "8D6pdYVRvF6RA3pntG56smWBZJ7MHAbHdMsk4BmCvL6JoneTC4aLFHRqyUGvSLgmcAv4VBET9Y31a4Ajpfw4Ek7DG5waDNktq5Ft1i57YoYPC91T7fVr2ZTkX7yfonFxuadnmWwMFQcLV1bpPTD9r7JFsKtKSXWTBozn4A5T7YHVys1PR25mgMdEsu5f1DDz6TM4XAM7LovcrRT5q9emcM5UPHQZc2JY7pYjH72mWrGMYcmkMwubUnJC5F3EeAurq9Z7LUmsizmGAuWqomtkjxSSQLpSoS5kWry3rtNAbvj8XR4DWKKbh4Qfepng1bqsCNRvhAf1JLWfekKciY9YuLKesf9kdP46oJEk7pzVni18ucRK1ETE2dCrQZekrk97sqZxGR4GD6wuGqBhMugyqQ5gApUEf3xxpdtj836zLA6VRVHL4Jc3ftmyXmwuC5d3wEdJqyxrATcdH8fbDu86kqvXCcm4WGgd3EbqQSDAgsXwdz617TvK6rwz2W9eXuBf6euwtNNLccSwUHj45EC9okiShob3R6oexhgB568Gprcw4rLRYNEgSULKEbazn"
    epochPrepAddress shouldEqual "Gxd4hMRT6H2cFm9DGywqDwCMYkpo2Zmz966CSB9WrbNcUXTdUw4d5RTpJTL3nn36Y7X7gcM1Lq8DCvf3ZkE6DXohUKikt2TTGbw8gQcBaKgaogzMfxEhHQVYkuHHNwJTQPeWqEz64tgGnwmkr2U6CKz5QWfCpErLBHgrBsH2Xy2dheJxp4cvXpU3tfDQse5otig9465bCQsZrMU85Ltjdpi2duCGrgyRvV1Aoe4wPdzCV7R3CEtmnoDFSw6g6tbkieLz9brrB81nFM3KCUeQTvKPn9kuUj6PKnuNsDeTfXe1u78khQcoEYZNVWoeX9KpiqNDmdWcYGhniHDSC3bVdDizsQxHC9hQ2YV4nUQjx1SsNjH96Dbcnz4hg3pDS9Z4EBWcX5Eb7yFoV7"
    dataPointAddress shouldEqual "jL2aaqw6XU61SZznwBKSYk5uhvGSniF7Wy4yzxFC26osVdyEHiDqzzpgpkVYg2GeCazyrreUj3R22S5SZSvXkcb1ATwcMzP9kCoCjmdqLYWuX5KankE8dFnv2D9aDyv7GuzEsgEmQbz9tDi4vyGom5FHdjUf4ZgJDm6WnP9sDhNpyV7mtBGaszNKHn211cKR68QmRsc73juzEo7UJEmK9TKkreevViv2L6M4qy25nWU4BYgkj8jjZ8HNjEPjR"
    poolDepositAddress shouldEqual "zLSQDVBaFFXNwLXqJmgrB9qkdfm66j3dHhH42d88WjkMYdmaJA2dMxQ38D51nVaPqKnNBnihXdY8mY98ktKMei7pgRDZEpT9Y6QJCr43TtiUexQcNjFq2BGHwqDQ5WLm3xvMQw2A2ZzCkkph4WN9gY5BHzyikXZYFPypaPKDnh2cs5F2Nith3cn7XzDRn1orcbbre3grtobazsFuShHYtkYQmf1pJAzvvMm28Kd9SfMKYAe7fZ"
    Blake2b256(epochPrepErgoTree.bytes).encodeHex shouldEqual "5d169dc11d12066e132ed7f305e5feb4b75f37987f4baada0d71a926abbc9ae4"
    Blake2b256(liveEpochErgoTree.bytes).encodeHex shouldEqual "91fecba1e2c6271108af4cb909ac17b3c62946801a364482f6d559e67b1e71e9"
    minPoolBoxValue shouldEqual 24000000
  }
}
