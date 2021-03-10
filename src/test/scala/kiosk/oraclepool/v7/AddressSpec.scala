package kiosk.oraclepool.v7

import kiosk.ergo._
import org.scalatest.{Matchers, PropSpec}
import scorex.crypto.hash.Blake2b256

class AddressSpec extends PropSpec with Matchers {
  lazy val minBoxValue = 2000000 // min value to remain AFTER paying rewards. For min value to keep BEFORE rewards, see minPoolBoxValue
  val epochPoolLive = new OraclePoolParams {}

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

    liveEpochAddress shouldEqual "8D6pdYVRvF6PeiuSWVBR4x2RqKKCjPPqMyTpmRho6EpfWMqnwN4uedKYYTkniyKPEjnjHXpLFLE8a7uszfA7VmsJSHiQJoUcvi8zEWMtfWsd4gpmZmAvttujn4Z4v6xBK3XkNv8DrCQAR3KGubrBFsWPS5G9KP6hCqWn7pt9giPrAfD4ZyNv7nQzesBcL3mu6tjQiHeMR84d9DVHUGEiKEeMmYC6wgTyPSGKfYJM3B5bjVhGqmUYTD6A4B6iubUng1jLVQ9EKgWPnFCH25PS85aCgfkZwyJjVhHTkjthJhNxB3RKvmihFbCnwhooXReqXZ4P5Y258VmvgiQHo58hamdTacYD45hPB4A7F9NivUm2hBwt6sXnxqZwU5NAh4wBUrgSRuLZeuPfQ6soRs3gaWPT6WgxowTt35D7ccHihqX4YmumJ1wNGbHEbTkAUKuc5bTawcT1kaKY6b44fLBRPVavDxkiEhrjVhFHQa2ZE9SxqUh4joRNQG9Ja7NQZn3DiNuADFMPp2FGSc4WPUZHs1EjqHVUhzgZnS6WWxPubEjjGJnftYejcu3415yB8"
    epochPrepAddress shouldEqual "2Qgfhy6m7AhxRU3xMHBXLtbXLWj6cs8cvSCsJdHh9rkZx64ZnzDDxBWBh5GsPkSuXcxfMGt36fLchZk2fMxXqxBzUcisq7HRo8kZA3Gp9fjqJYZAtuomUSkirWTEiM6X8kgahypcsRAvEt2PcXX2BLcJo1VfR7XggvhnNLCTqBxRDaWMVsERXVWVuoEU3DPNz67EVVnRdEqpuMUo7yoGZCSDufujyWHWszNmdCMEDBuF9WDEw3efnFMg8W81AYZJZcnAggfoZNtpjYx7stN26GRvW32m9x8AnW2Sz8gigg3rTyhavPFgwA2D1W59UQ1pTP2Dgb5kDT1yRi42Q5uimXaAbzGLqhz2tMmh1ds5X7N9LbKNnoyqe9N9agJ7GVeFBt6VUYAmfwuguWrgeA62kk2qerckhNdyBPjwSQmKMzomnwxkNZ5sKwgAAnjzMwfyJwbyueACgFe4dWomTSEXEDFBG4gqCTW2NhGw1p"
    dataPointAddress shouldEqual "AucEQEJ3Y5Uhmu4o8rZ7PsdZ3VUFyEVZia4n77jTZrT4aPFMpY2Zn63HEEhgZDL1dMgX4UmTqH5Uwnd3i1hjeuK8kb8GiHsZvJzN78iuThS4sK4nNyhKz5piRGeFFQUGbi6WMu7QoM7CZRvxsZWJCV8RnEJLbMk5aSQ5ukhgWniqKFo6Ka7nuUCTEtqDaZEeJER5"
    poolDepositAddress shouldEqual "zLSQDVBaL7d8mjWMSTdbA5DReg1kqMxp5FW8patzNCziV2p2z4xzi3uSeX9Y8mcPhfC7i6its7uR4mNxU1ogLXwipePGBj9FTmZkhoXQ5qe9dVGsqDk3wMpdGq6xseNJi1Hq8ntbuXdXxfeBmE7jTFXaAL3mMMmR2nUksU167XjGYHp9ho5WkpnjRcP1Zn34goNFkvNiXa4NU2irPpLk1kYdQdm6neJvdtNYFRvCLvoPhdpVVV"
    updateAddress shouldEqual "9vQZrzvghythEub2uuGhjKFGrPeC8cmxzRsngeojKT1zji7TjQiGBhkNkXbRbMKTt4aidvMYQifLiJXdoXKRqPHwjzcmU2KEMjnY2jBzb5xPHqqSMm1fgV5BK1sdjw67U8nMygZtyKfr6LNGCxSLQZ7ythDoaMWjgKZZLh5wHhgdxJWrn7oPmRTVf3z3ogjd78omiXPmhfLoC6yUajmCn1qhjk197YCHocXaUAvEvutHf9jiFQPWBxa5frry6eKSRVoWMwngaqwgrzTMyH9b7qAnCkMDxzYeCj2c7q1FCoH9vTwra8c5NiKrnEcWaP8AFwLxnoZMF9rogj6RVBTSXvKCsZiGZCvE9bT9rYvug5uyo2fuXkxPvGVGGkQa7buDCMonihZUeK2KTFyBtuvFTodCJXLkkxVLcxRorfpK7gNnp1St93iJh5Dg8hkjCeEvqMXb5KQ1VGP2m9sYjws6C9NbZvTNKFkohLv5ZwexUSU2KbAs2zQDytv9FwyZRgP5uSphBzoBWDiX1QEwv18ryFovCqTq4pU31or"
    Blake2b256(epochPrepErgoTree.bytes).encodeHex shouldEqual "ef45eef675d34d2347148b05346729328790d98d8b4ee95c11940b1d855666e1"
    Blake2b256(liveEpochErgoTree.bytes).encodeHex shouldEqual "4eb835eade4cde59b0773c9182f41cdea42170953e90cdbdf74abec34bcf438d"
    minPoolBoxValue shouldEqual 132000000
  }
}
