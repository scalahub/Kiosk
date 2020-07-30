package kiosk.oraclepool.v2

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

    liveEpochAddress shouldEqual "F7f7vfC28mjet2UuH3cKyL7CHbtnc1AgxnEBQ6UhMvRtxhX7BrG7MLhMj7JEcmMyQqRzHg7hfoLNSzoDWg4PWfqSxoZXkTBPUWharJCtoRjaoHGYgjF9BJCjDNR13EwMVoXBhY2gmgfWyCjKjncFpjzbSBQYRAsj7W5vg3A2NtXudGMn2YjfHSqjFk1xzV4sfYGtfM9fLfd3ZEBMFfQpPRapG4DXaGL8emVrRsqfjGDqVRkxw1kJyffbFTsDStRgrKeGbA1gZKsKAYJiWLYVbmndRxhUuM7fQhtX8qzRMpDfqti43eotgxVXU5pr9Q7a4Pv2VbvS8gBDceRPZeLdsxBiDoWVbGEkF8vB7QrDNr9YxXEob4KircTpECARmcGgeLCHwr2i7AMGbs2tFFLX7PoHyYRv3ertFGS1CEth6wnjmo3SEjK8HXU"
    epochPrepAddress shouldEqual "8wwE3iC8YATKziCfDcjKi9vHD7ga4gqYi5Wy6mHKpXSh73hLGi9vSFeC1JDRRnvyMUydfyME1ziBBAsj4vyHLJT2gQP3U6vutaZBaB9VBYzihkjiAhm9hEwzMn9GEruW6PvCAH6W2HczhwRUvmZJjp9L4DyhCxPjkGJKYJ5YBagXgxU9MtRvuCddtywchsVUGZyxr1RfhXHtFY4o3m8Nhvj7emmPdwBBoRrcc7f8NQVUFbDrtTXFz1vs2L4nTLxWrFTBvqmjDwfptkpHZaUCCNGPbgP8jb1ateUFBMH7zou9MQbJ8pb4JkwArrhHbgRjJg2iqyvFEQE98uotEowXrj6XLtu7MGWyXbXqqM3hh2S7U81jebyEJQpEPMC5wsEPSSQgURSe9mUQhfiSsECaT"
    dataPointAddress shouldEqual "jL2aaqw6XU61SZznxeykLpREPzSmZv8bwbjEsJD6DMfXQLgBc12wMmPpVD81JnLvRbMphA3SehsyWc4kQ88uKa9SVA3EikNeTUGGQquabVkR4rvvbHgczZPtLhkrmsfE1yLuLFtwBUwuvuEAS4fHHt5ygRC5g3VbsNBhd5oqZGZgmhjgk1zUWLQy6V8zs4K3RxEuEdFWQ58JSBQu8EaR4TnUeAnGyG8Atapku6woNAAUKmT8Vtg6ikEauDY5m"
    poolDepositAddress shouldEqual "zLSQDVBaHgffKUvBt3Gvth2DTW32DGbDmg5nLxVM9Ye2svJzqThs9MUwcMEjc63ofKvaxF5Mrin8Cjk5Dvmd5D43dDyuUZUXpAA4nSq4tEryxbCbyVspjuCbweE8BidFg8SH4EShBVg6Do7nfB85kYMTkDoL2iXBiYzQxZ8GbMwQimR9LfwSn3vbjX2kSy8pihSC1SJupkq9QySUAUfHeY5B56Le1UdYVQMxJiE5hr5eT1968X"
    Blake2b256(epochPrepErgoTree.bytes).encodeHex shouldEqual "a63aa2ea3aebfeace94468359491c5caa53c188ebed42a5d806d0013fd01d5e8"
  }
}
