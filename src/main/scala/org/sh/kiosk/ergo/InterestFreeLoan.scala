package org.sh.kiosk.ergo

//import org.bouncycastle.jcajce.provider.digest.Blake2b.Blake2b256
import org.ergoplatform.Pay2SAddress
import org.sh.kiosk.ergo.ErgoMix.$ergoScript
import scorex.crypto.hash.Blake2b256

object InterestFreeLoan {
  /* using the description at https://www.ergoforum.org/t/interest-free-loan-contract/67

     Alice wants to borrow 10000 Euros using Ergs as collateral
     Bob wants to lend 10000 Euros to Alice against installments of 200 Euro per month for 50 months
     (totalling 200*50 = 10000 Euros)

     The amount of collateral is adjusted based on the amount still owed.
     At any time, the collateral must be 1.2 times the value of amount pending to be paid to Bob

     For each repayment, Alice and Bob jointly release


   */
  val rateOracleTokenID:Array[Byte] = Blake2b256("hello").toArray

  val $ergoScript = new ErgoScript {}
  $ergoScript.env_setCollByte("rateOracleTokenID", rateOracleTokenID)

  val alicePrivateKey = $ergoScript.$getRandomBigInt

  import ErgoScriptUtil._

  val alice = hexToGroupElement($ergoScript.$getGroupElement(alicePrivateKey))

  val bobPrivateKey = $ergoScript.$getRandomBigInt
  val bob = hexToGroupElement($ergoScript.$getGroupElement(bobPrivateKey))

  $ergoScript.env_setGroupElement("alice", alice)
  $ergoScript.env_setGroupElement("bob", bob)
  val loanBoxSource =
    """{
      |  val dataInput = CONTEXT.dataInputs(0)
      |  val currentEuros = SELF.R4[Long].get // how many Euros pending
      |  val rate = dataInput.R4[Long].get // rate (how many Euros for 1 ERG)
      |  val correctToken = dataInput.tokens(0)._1 == rateOracleTokenID
      |  val out = OUTPUTS(0) // should be same box script
      |
      |  val correctScript = out.propositionBytes == SELF.propositionBytes
      |  val outEuros = out.R4[Long].get
      |  val euroDiff = currentEuros - outEuros
      |  val ergsDiff = SELF.value - out.value
      |  val correctDiff = euroDiff * rate >= ergsDiff
      |  // todo add more logic (profit sharing by Alice and Bob when Euro price drops)
      |
      |  correctDiff && correctScript && correctToken && proveDlog(alice) && proveDlog(bob)
      |}""".stripMargin

  val loanBoxScript = $ergoScript.$compile(loanBoxSource)

  import $ergoScript.$ergoAddressEncoder

  val loanBoxAddress = Pay2SAddress(loanBoxScript)
}
