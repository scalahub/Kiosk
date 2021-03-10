package kiosk.oraclepool.v7

trait OraclePoolParams extends Contracts {
  lazy val minBoxValue = 1000000 // min value to remain AFTER paying rewards. For min value to keep BEFORE rewards, see minPoolBoxValue
  override def livePeriod = 3 // blocks
  override def prepPeriod = 2 // blocks
  override def buffer = 2 // blocks
  override def maxDeviation: Int = 5 // percent
  override def minOracleBoxes: Int = 1
  /*
    poolNFT           008a94c8c76bbaa1f0a346697d1794eb31d94b37e5533af9cc0b6932bf159339
    12 oracle Tokens  5579de48d16e54ddb34df789d418e1f10c119e15a824ea32dc21696c067f9fbe
    updateNFT         7b8e292c4a89efb509c89b10111468223678e0a855b20607a0b9fce80a9af694
    12 ballot tokens  f502b38d40208f8faf0e4dd9f74952e17b24d8f96abb9c9f57f8e961d25a8e37
   */
  lazy val oracleToken = "5579de48d16e54ddb34df789d418e1f10c119e15a824ea32dc21696c067f9fbe"
  lazy val poolNFT = "008a94c8c76bbaa1f0a346697d1794eb31d94b37e5533af9cc0b6932bf159339"
  lazy val updateNFT = "7b8e292c4a89efb509c89b10111468223678e0a855b20607a0b9fce80a9af694"
  lazy val ballotToken = "f502b38d40208f8faf0e4dd9f74952e17b24d8f96abb9c9f57f8e961d25a8e37"

  override def oracleReward = 1000000 // Nano ergs. One reward per data point to be paid to oracle
  lazy val maxNumOracles = 12
  lazy val minVotes = 6
  lazy val minStorageRent = 10000000L

  override def minPoolBoxValue: Long = oracleReward * (maxNumOracles + 1) + minBoxValue // min value allowed in live epoch box
}
