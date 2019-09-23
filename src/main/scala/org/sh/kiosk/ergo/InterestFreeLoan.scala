package org.sh.kiosk.ergo

//import org.bouncycastle.jcajce.provider.digest.Blake2b.Blake2b256
import org.ergoplatform.{Pay2SAddress, Pay2SHAddress}
import org.sh.kiosk.ergo.ErgoMix.$ergoScript
import org.sh.kiosk.ergo.util.ErgoScriptUtil._
import org.sh.cryptonode.util.BytesUtil._

import scorex.crypto.hash.Blake2b256
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer

object InterestFreeLoan {
  /* using the description at https://www.ergoforum.org/t/interest-free-loan-contract/67

     Alice wants to borrow 10000 Euros using Ergs as collateral
     Bob wants to lend 10000 Euros to Alice against installments of 200 Euro per month for 50 months
     (totalling 200*50 = 10000 Euros)

     The amount of collateral is adjusted based on the amount still owed.
     At any time, the collateral must be 1.2 times the value of amount pending to be paid to Bob

     For simplicity, we assume that the loan is paid back by Alice in tokens tethered to Euros.
     These tokens (with tokenID euroToken below) are assumed to be exchangeable by the lender and
     borrower at the rate of 1 token per euro.


   */
  val $startRate = 10 // 10 euros per erg

  val $rateOracleTokenID:Array[Byte] = Blake2b256("rate").toArray
  val $euroTokenID:Array[Byte] = Blake2b256("euro").toArray


  val $ergoScript = new ErgoScript {}
  $ergoScript.env_setCollByte("rateOracleTokenID", $rateOracleTokenID)
  $ergoScript.env_setCollByte("euroTokenID", $euroTokenID)

  // borrower
  val $alicePrivateKey = getRandomBigInt
  val alice = hexToGroupElement($ergoScript.$getGroupElement($alicePrivateKey))

  // lender
  val $bobPrivateKey = getRandomBigInt
  val bob = hexToGroupElement($ergoScript.$getGroupElement($bobPrivateKey))

  $ergoScript.env_setGroupElement("alice", alice)
  $ergoScript.env_setGroupElement("bob", bob)
  $ergoScript.env_setInt("startRate", $startRate)

  val env = $ergoScript.env_get
  val ergoScript =
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

  val $ergoTree = $ergoScript.$compile(ergoScript)

  val serializedScript = {
    $ergoScript.$env.map{
      case (keyword, value) =>
        keyword + " = " + serialize(value).encodeHex
    }.toArray ++ Array(
      $ergoScript.$matchScript(DefaultSerializer.serializeErgoTree($ergoTree), $ergoScript.$env.keys.toArray).grouped(120).mkString("\n")
    )
  }
  import $ergoScript.$ergoAddressEncoder

  val boxAddress = {
    Array(
      "p2s: "+Pay2SAddress($ergoTree),
      "p2sh: "+Pay2SHAddress($ergoTree)
    )
  }
}
