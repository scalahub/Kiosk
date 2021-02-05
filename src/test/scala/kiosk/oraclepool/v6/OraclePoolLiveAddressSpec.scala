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

    liveEpochAddress shouldEqual "8D6pdYVRvF6PeiuSWVBR4x2RqKKCjPPqMyTpmRho6EpfWMqnwN4uedKYYTkniyKPEjnjHXpLFLD9HaVCYV7vkSNeRoA4zSnPj2fzcTbNpKtTqVZwGLTrNnA413bzvdeJvewhe5oB4oCiuvjctT8j2i7qfMjbvQeMmBd8Nct6BJ5s3e56ZTb4qvmkUZkyMNvzXXnt2wMV9GmPyYuoEc82tf4j96xdH6aXfCvC6v7sgW5Zautw2nQMdgQkG5RwaaQYQ7Swio8RZSWWgePs1CdWzih94oPNkgcLddCAYwK263tGibqAurtvcpTC1ipMWE8Xt9rMBZH2JpW4yQseqRBFsDdfyS45A6chMFKLgEobzmyc9pLWAzB6qbwAEsyhmUtPPcyQzTomSfh8gMYHViuAWB8xM4FJLgWbfGizvCzdAP56HXx5DJV8Gu9vmordM5JxYPn4aHZX9pbAsQuTLgXCjEhEr1UaXBUzrk7reggocnWv84jkSYzhtDKKAzpJX4cT8WLpQ3R1ZMLBb2vu1b3BhcAosKuyvKvrH35YyZchqtuaEVRTmQYXFPRH4tFjL"
    epochPrepAddress shouldEqual "7DY5qpcRgHc59umTtm5sX1rNQx9ZpVWN2jqKxDbonmF5FZEWzmXgV4xpD8hpnFLccFV6MbPikT9AJHsycW57Y3cYxPUfy4X7N8zJvHACmMiw2GwHyrCwHybTqGS3XePjiCppfP2SoMthVHBpCy7CLiwTicLA1L7WHrLMtLGiq9Y2rrtgwCBKiUqw19H1BEBVrRg2f2fQuzSEp37aLrtZXG6D7CjjVGCJyWPS3ELSge9C1UCpPi5DyW1e22v83azKgN2cbbK3akivx6AwLiMxAoJU4RLeiTJZmprPQbDuZHVSzTgCeFGkiwLe967zWvqtJrBfTLVPRVj2YmRa2P544ETfrK8M3htGrNYix59rfsiHpfyVbwPoNdxrCEiE93myG5nD2fEPaERywKyJtbf1xmjWGjYmpXi6YTKKxm4Gnw7pLpRpkU9eKNcadWAcMAdUofyxr2aqWoWii41WP64zUy9nVvgCgQ1CrwGVpok"
    dataPointAddress shouldEqual "jL2aaqw6XU61SZznwZeLEryve79s26LKMtJDCG5AMu3jQ8oUxQ8wREoWLPepqTWe1MThyMvztVpps3Fq7mhoWovJVuEeN567LSn5HWjX1PmVArn4NCqPoHducGZmPC1NYWvEXNhXXn5xmebKHowaWHCgQE1876EM3jPCzzvrqNthJtq46jq4qvCqhwep64P4AbbrT5yoRwsT9CCkqPAtx7ZbPpwc4sLHHTTdracdejaEQz1gmoHSMLH9n9auV"
    poolDepositAddress shouldEqual "zLSQDVBaK3znGzoLZXSxu7Gu7XruknoZfoPdGkEKBP8san8qnxMfVG5eQCPVjRVpZ41Vk4bzUr6nT9XjteEQwomUhixq1idPcGrvMoXUAaVL8wGoP1LqnF8MNKnU398vKHcwgVXZLWyHQW2NoyhPSkaj3rk7apjE9Hansio6X73dixJSaDdYXfrGHqj4dzbxZfgxZYswZx6XbJmG9pN67zZKE8u3pJ8FD5p2MwUHBo4KHCskvg"
    updateAddress shouldEqual "9vQZrzvghythEub2uuGhjKFGrPeC8cmxzRsngeojKT1zji7TjQiGBhkNkXbRbMKTt4aidvMYQifLiJXdoXKRqPHwjzcmU2KEMjnY2jBzb5xPHqqSMm1fgV5BK1sdjw67U8nMygZtyKfr6LNGCxSLQZ7ythDoaMWjgKZZLh5wHhgdxJWrn7oPmRTVf3z3ogjd78omiXPmhfLoC6yUajmCn1qhjk197YCHocXaUAvEvutHf9jiFQPWBxa5frry6eKSRVoWMwngaqwgrzTMyH9b7qAnCkMDxzYeCj2c7q1FCoH9vTwra8c5NiKrnEcWaP8AFwLxnoZMF9rogj6RVBTSXvKCsZiGZCvE9bT9rYvug5uyo2fuXkxPvGVGGkQa7buDCMonihZUeK2KTFyBtuvFTodCJXLkkxVLcxRorfpK7gNnp1St93iJh5Dg8hkjCeEvqMXb5KQ1VGP2m9sYjws6C9NbZvTNKFkohLv5ZwexUSU2KbAs2zQDytv9FwyZRgP5uSphBzoBWDiX1QEwv18ryFovCqTq4pU31or"
    Blake2b256(epochPrepErgoTree.bytes).encodeHex shouldEqual "cf568476e923cffe74c80ffb4e5a6744f503115382e680f75770161b0d773f8f"
    Blake2b256(liveEpochErgoTree.bytes).encodeHex shouldEqual "b344353a8a03e3d4c2791830592eb4c6fc7c7406234986f9322566d8e2635511"
    minPoolBoxValue shouldEqual 262000000
  }
}
