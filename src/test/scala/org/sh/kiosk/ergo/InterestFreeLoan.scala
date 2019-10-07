package org.sh.kiosk.ergo

import org.ergoplatform.{Pay2SAddress, Pay2SHAddress}
import org.sh.kiosk.ergo.ErgoMix.$ergoScript
import org.sh.kiosk.ergo.util.ErgoScriptUtil._
import org.sh.cryptonode.util.BytesUtil._

import scorex.crypto.hash.Blake2b256
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer

object InterestFreeLoan extends App {
  /* using the description at https://www.ergoforum.org/t/interest-free-loan-contract/67

     Alice wants to borrow 10000 Euros using Ergs as collateral
     Bob wants to lend 10000 Euros to Alice against installments of 200 Euro per month for 50 months
     (totalling 200*50 = 10000 Euros)

     The amount of collateral is adjusted based on the amount still owed.
     At any time, the collateral must be 1.2 times the value of amount pending to be paid to Bob

     For simplicity, we assume that the loan is paid back by Alice in tokens tethered to Euros.
     These tokens (with tokenID euroToken below) are assumed to be exchangeable by the lender and
     borrower at the rate of 1 token per euro.

     To summarize:
      Loan will be given in actual Euros (with offchain trusted setup)
      Collateral will be in Euros
      Repayment will be in secondary tokens tethered to Euros. This is called equal monthly installments (emi)


   */
  val $startRate = 10 // 10 euros per erg

  val $rateOracleTokenID:Array[Byte] = Blake2b256("rate").toArray
  val $euroTokenID:Array[Byte] = Blake2b256("euro").toArray


  val $env = new Env
  val $ergoScript = new ErgoScript($env) {}
  $ergoScript.$myEnv.setCollByte("rateOracleTokenID", $rateOracleTokenID)
  $ergoScript.$myEnv.setCollByte("euroTokenID", $euroTokenID)

  // borrower
  val $alicePrivateKey = getRandomBigInt
  val alice = hexToGroupElement($env.getGroupElement($alicePrivateKey))

  // lender
  val $bobPrivateKey = getRandomBigInt
  val bob = hexToGroupElement($env.getGroupElement($bobPrivateKey))
  val $oneMonth = 720*30 // 720 blocks per day
  val $fiveDays = 720*5 // 720 blocks per day
  val $emi = 1000 // Euros per month // equal monthly installment

  val $startDate = 10000 // block height at which loan was started
  $ergoScript.$myEnv.setGroupElement("alice", alice)
  $ergoScript.$myEnv.setGroupElement("bob", bob)
  $ergoScript.$myEnv.setInt("startRate", $startRate)
  $ergoScript.$myEnv.setInt("startDate", $startDate)
  $ergoScript.$myEnv.setInt("oneMonth", $oneMonth)
  $ergoScript.$myEnv.setInt("fiveDays", $fiveDays)
  $ergoScript.$myEnv.setInt("emi", $emi)


  val env = $ergoScript.$myEnv.getAll
  val ergoScript =
    """{
      |  val dataInput = CONTEXT.dataInputs(0)
      |  val currentEuros = SELF.R4[Long].get // how many Euros pending
      |  val rate = dataInput.R4[Long].get // rate (how many Euros for 1 ERG)
      |  val correctRateOracle = dataInput.tokens(0)._1 == rateOracleTokenID
      |  val out = OUTPUTS(0) // should be same box script
      |
      |  val lastPaymentHeight = SELF.R5[Int].get // what height last payment was made
      |  val thisPaymentHeight = out.R5[Int].get // what the current height is
      |  val correctHeight = thisPaymentHeight <= HEIGHT && (thisPaymentHeight - lastPaymentHeight > oneMonth)
      |
      |  val correctScript = out.propositionBytes == SELF.propositionBytes
      |
      |  val outEuros = out.R4[Long].get
      |  val euroDiff = currentEuros - outEuros
      |  val ergsDiff = SELF.value - out.value
      |  val correctErgsDiff = euroDiff * rate >= ergsDiff
      |
      |  val correctDiff =  euroDiff == emi && correctErgsDiff
      |
      |  val emiBox = OUTPUTS(1) // this is the box where Alice will pay to Bob
      |  val correctEmiAmt = emiBox.tokens(0)._1 == euroTokenID && emiBox.tokens(0)._2 >= emi
      |  val correctEmiScript = emiBox.propositionBytes == proveDlog(bob).propBytes
      |  val correctEmi = correctEmiAmt && correctEmiScript
      |
      |  val nonPayment = (HEIGHT - lastPaymentHeight) > (oneMonth + fiveDays)
      |
      |  // todo add more logic (profit sharing by Alice and Bob when Euro price drops)
      |  val correctTx = correctDiff && correctScript && correctRateOracle && correctHeight
      |
      |  correctTx && ((proveDlog(alice) && correctEmi) || (proveDlog(bob) && nonPayment))
      |
      |}""".stripMargin

  val $ergoTree = $ergoScript.$compile(ergoScript)

  val serializedScript = {
    $ergoScript.$myEnv.$getEnv.map{
      case (keyword, value) =>
        keyword + " = " + serialize(value).encodeHex
    }.toArray ++ Array(
      $ergoScript.$matchScript(DefaultSerializer.serializeErgoTree($ergoTree), $ergoScript.$myEnv.$getEnv.keys.toArray).grouped(120).mkString("\n")
    )
  }

  import $ergoScript.$ergoAddressEncoder

  val boxAddress = {
    Array(
      "p2s: "+Pay2SAddress($ergoTree),
      "p2sh: "+Pay2SHAddress($ergoTree)
    )
  }

  boxAddress foreach println
}
