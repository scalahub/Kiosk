package kiosk.oraclepool.v5

import kiosk.ergo._
import org.scalatest.{Matchers, PropSpec}
import scorex.crypto.hash.Blake2b256

trait EpochPoolLive extends EpochPool {
  lazy val minBoxValue = 2000000 // min value to remain AFTER paying rewards. For min value to keep BEFORE rewards, see minPoolBoxValue
  override def livePeriod = 20 // blocks
  override def prepPeriod = 10 // blocks
  override def buffer = 4 // blocks
  override def maxDeviation: Int = 5 // percent
  override def minOracleBoxes: Int = 4

  /*
    poolNFT 54acaa0c6d5d3bc66b88364a423b5f156ed763f7236d437adb44d70787bc0f95
      issued in tx https://explorer.ergoplatform.com/en/transactions/2a895962b963cffca90cee85d4cfe8b81ee8f60a081fcc9851a8136e0a26918b
      (currently stored in box id 0835fcce3cb95674baba88fd67b141b9968e285a34c7e32728c707819a588d7a)

    12 oracle Tokens 78180c57b903b2d144f60b61ab5256e97f1bfcd7fec65c1c86dc83854230a5d9
      issued in tx https://explorer.ergoplatform.com/en/transactions/a42f71cf4476512418526eee671b476c0c8460289a6b8bff68b3c8af9e1ba9d6
      (currently stored in box Id 2b3c2b5d2a2135a8f038aff51d065f14c5670e62cbab6fc6d708bb851f436a20)

    updateNFT 00483e0ed4a9faba9f6e6c9629d9baca9751316639445ff6ecabd90fb2afa025
      issued in tx https://explorer.ergoplatform.com/en/transactions/611406f98b7a9c651398f9a08550b1a35f6a881eb26db6069ba939a9b96880c4
      (currently stored in box id 6ea37466d4602d1973d09c93b534ad33d5b2ccb1ab023eba3e987a52fe0bae2d)

    12 ballot tokens 004b2ca8adbcf9f15c1149fab8264dbcafbdd3c784bb4fae7ee549c16774914b
      issued in tx https://explorer.ergoplatform.com/en/transactions/21eef89d3eda18433286ced705d86eab25f495902e67e1c457dc424a952f2475
      (currently stored in box id 976b43ad6c2a47396da51cfa8dfec83ed5b1a078ded44f358c6952a83487ec60)

   */
  lazy val oracleToken = "78180c57b903b2d144f60b61ab5256e97f1bfcd7fec65c1c86dc83854230a5d9"
  lazy val poolToken = "54acaa0c6d5d3bc66b88364a423b5f156ed763f7236d437adb44d70787bc0f95"
  lazy val updateNFT = "00483e0ed4a9faba9f6e6c9629d9baca9751316639445ff6ecabd90fb2afa025".decodeHex
  lazy val ballotTokenId = "004b2ca8adbcf9f15c1149fab8264dbcafbdd3c784bb4fae7ee549c16774914b".decodeHex

  override def oracleTokenId: Array[Byte] = oracleToken.decodeHex
  override def poolNFT: Array[Byte] = poolToken.decodeHex

  override def oracleReward = 20000000 // Nano ergs. One reward per data point to be paid to oracle
  lazy val maxNumOracles = 12
  lazy val minVotes = 6
  lazy val minStorageRent = 10000000L

  override def minPoolBoxValue: Amount = oracleReward * (maxNumOracles + 1) + minBoxValue // min value allowed in live epoch box
}
