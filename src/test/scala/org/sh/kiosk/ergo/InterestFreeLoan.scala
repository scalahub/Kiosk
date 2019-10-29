package org.sh.kiosk.ergo

import org.ergoplatform.{Pay2SAddress, Pay2SHAddress}
import org.sh.kiosk.ergo.ErgoMix.$ergoScript
import org.sh.kiosk.ergo.util.ErgoScriptUtil._
import org.sh.cryptonode.util.BytesUtil._

import scorex.crypto.hash.Blake2b256
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer

object InterestFreeLoan extends App {
  /* using the description at https://www.ergoforum.org/t/interest-free-loan-contract/67

     Alice wants to borrow 100 USD using Ergs as collateral
     Bob wants to lend 100 USD to Alice against installments of 10 USD per month for 10 months

     The amount of collateral is adjusted based on the amount still owed.
     At any time, the collateral must be 1.2 times the value of amount pending to be paid to Bob

     For simplicity, we assume that the loan is paid back by Alice in tokens tethered to USD
     These tokens (with tokenID euroToken below) are assumed to be exchangeable by the lender and
     borrower at the rate of 1 token per USD.

     To summarize:
      Loan will be given in actual USD (with offchain trusted setup)
      Collateral will be in Ergs
      Repayment will be in secondary tokens tethered to USD.

   */
  runTest
  def runTest = {
    val rateOracleTokenID:Array[Byte] = Blake2b256("rate").toArray
    val usdTokenID:Array[Byte] = Blake2b256("USD").toArray // bob's one-way USD token

    val env = new Env
    env.setCollByte("rateOracleTokenID", rateOracleTokenID)
    env.setCollByte("usdTokenID", usdTokenID)

    // borrower
    val alicePrivateKey = getRandomBigInt
    val alice = hexToGroupElement(ECC.gExp(alicePrivateKey))

    // lender
    val bobPrivateKey = getRandomBigInt
    val bob = hexToGroupElement(ECC.gExp(bobPrivateKey))

    val oneMonth = 720*30 // 720 blocks per day
    val fiveDays = 720*5 // 720 blocks per day

    val startDate = 10000 // block height at which loan was started

    env.setGroupElement("alice", alice)
    env.setGroupElement("bob", bob)
    env.setInt("oneMonth", oneMonth)
    env.setInt("fiveDays", fiveDays)
    env.setInt("emi", 1000) // equal monthly installment in USD cents


    val ergoScript = new ErgoScript(env) {}

    val src =
      """{
        |  val dataInput = CONTEXT.dataInputs(0)
        |  val rate = dataInput.R4[Long].get // rate (how many USD for 1 ERG)
        |  val correctRateOracle = dataInput.tokens(0)._1 == rateOracleTokenID
        |
        |  val out = OUTPUTS(0) // should be same box script
        |  val currentUSD = SELF.R4[Long].get // how many USD owed to Bob
        |
        |  val lastPaymentHeight = SELF.R5[Int].get // what height last payment was made
        |  val thisPaymentHeight = out.R5[Int].get // what the current height is
        |
        |  val correctHeight = thisPaymentHeight <= HEIGHT &&
        |                      thisPaymentHeight >= HEIGHT - 720 && // within a day
        |                      thisPaymentHeight > lastPaymentHeight
        |
        |  val correctScript = out.propositionBytes == SELF.propositionBytes
        |
        |  val outUSD = out.R4[Long].get
        |  val usdDiff = currentUSD - outUSD
        |  val ergsDiff = SELF.value - out.value
        |  val correctErgsDiff = usdDiff * rate == ergsDiff
        |
        |  val correctDiff = usdDiff == emi && correctErgsDiff
        |
        |  val bobBox = OUTPUTS(1) // this is the box where Alice will pay to Bob
        |  val correctBobAmt = bobBox.tokens(0)._1 == usdTokenID && bobBox.tokens(0)._2 == emi
        |  val correctBobScript = bobBox.propositionBytes == proveDlog(bob).propBytes
        |  val correctBobBox = correctBobAmt && correctBobScript
        |
        |  val nonPayment = (HEIGHT - lastPaymentHeight) > (oneMonth + fiveDays)
        |
        |  val correctTx = correctDiff && correctScript && correctRateOracle && correctHeight
        |
        |  val marginCall = currentUSD * rate > SELF.value
        |
        |  (marginCall && proveDlog(bob)) || (correctTx && ((proveDlog(alice) && correctBobBox) || (proveDlog(bob) && nonPayment)))
        |
        |}""".stripMargin

    val ergoTree = ergoScript.$compile(src)

    import ergoScript.$ergoAddressEncoder

    println(Pay2SAddress(ergoTree))
  }

}
