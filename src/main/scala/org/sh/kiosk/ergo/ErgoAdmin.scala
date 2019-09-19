package org.sh.kiosk.ergo

import org.sh.easyweb.Text
import org.sh.kiosk.ergo.ErgoAPI._

object ErgoAdmin extends App {
  //val objects = List(ErgoScript, ErgoAPI, Peers, Wallet, Mining, Info, Utils, Transactions)
  val objects = List(ErgoScriptDemo, ErgoMix, InterestFreeLoan, ErgoAPI, Peers, Wallet, Mining, Info, Utils, Transactions) //, ErgoAPI, Peers, Wallet, Mining, Info, Utils, Transactions)
  new org.sh.easyweb.AutoWeb(objects, "ErgoDemo")
}

object Peers {
  // vars, vals and defs starting with '$' won't appear in HTML
  //  def all = $q("peers/all", false, Get, Nil)
  //
  //  def blacklisted = $q("peers/blacklisted", false, Get, Nil)

  def connected = $q("peers/connected", false, Get, Nil)

  def connect(url:String) = $q("peers/connect", false, PostJsonRaw, Nil, Some(url))
}

object Wallet {
  // vars, vals and defs starting with '' won't appear in HTML
  def init(pass: String, optional_mnemonicPass: Option[String]) = $q("wallet/init", true, PostJson,
    Seq("pass" -> pass) ++ optional_mnemonicPass.map{ mnemonicPass => "mnemonicPass" -> mnemonicPass}
  )

  def restore(pass: String, mnemonic: String, optional_mnemonicPass: Option[String]) = $q("wallet/restore", true, PostJson,
    Seq("pass" -> pass, "mnemonic" -> mnemonic) ++ optional_mnemonicPass.map{ mnemonicPass => "mnemonicPass" -> mnemonicPass}
  )

  def unlock(pass: String) = $q("wallet/unlock", true, PostJson,
    Seq("pass" -> pass)
  )

  def lock = $q("wallet/lock", true, Get, Nil)

  def deriveKey(derivationPath:String) = {
    $q("wallet/deriveKey", true, PostJson, Seq("derivationPath" -> derivationPath))
  }

  def deriveNextKey = {
    $q("wallet/deriveNextKey", true, Get, Nil)
  }

  def balances = {
    val $INFO$ = "Get total amount of confirmed Ergo tokens and assets"
    $q("wallet/balances", true, Get, Nil)
  }

  def transactions(minInclusionHeight:Int, maxInclusionHeight:Int, minConfirmations:Int, maxConfirmations:Int) = {
    //curl -X GET "http://127.0.0.1:9052/wallet/transactions?minInclusionHeight=10&maxInclusionHeight=100" -H "accept: application/json" -H "api_key: hello"
    require(maxInclusionHeight >= minInclusionHeight, s"maxInclusionHeight ($maxInclusionHeight) must be greater than minInclusionHeight ($minInclusionHeight)")
    require(maxConfirmations > minConfirmations, s"maxConfirmations ($maxConfirmations) must be greater than minConfirmations ($minConfirmations)")
    $q("wallet/transactions", true, Get,
      Seq(
        "minInclusionHeight" -> minInclusionHeight.toString,
        "maxInclusionHeight" -> maxInclusionHeight.toString,
        "minConfirmations" -> minConfirmations.toString,
        "maxConfirmations" -> maxConfirmations.toString
      )
    )
  }

  def boxes(minConfirmations:Int, minInclusionHeight:Int) = {
    // curl -X GET "http://127.0.0.1:9052/wallet/boxes?minConfirmations=19&minInclusionHeight=21" -H "accept: application/json" -H "api_key: hello"
    $q("wallet/boxes", true, Get,
      Seq(
        "minConfirmations" -> minConfirmations.toString,
        "minInclusionHeight" -> minInclusionHeight.toString
      )
    )
  }

  def boxes_unspent(minConfirmations:Int, minInclusionHeight:Int) = {
    // curl -X GET "http://127.0.0.1:9052/wallet/boxes?minConfirmations=19&minInclusionHeight=21" -H "accept: application/json" -H "api_key: hello"
    $q("wallet/boxes/unspent", true, Get,
      Seq(
        "minConfirmations" -> minConfirmations.toString,
        "minInclusionHeight" -> minInclusionHeight.toString
      )
    )
  }

//  def balances_with_unconfirmed= $q("wallet/balances/with_unconfirmed", true, Get, Nil)

  def addresses = $q("wallet/addresses", true, Get, Nil)

//  def p2s_address(source: Text) =
//    $q("wallet/p2s_address", true, PostJson, Seq("source" -> source.getText))

//  def p2sh_address(source: Text) =
//    $q("wallet/p2sh_address", true, PostJson, Seq("source" -> source.getText))
}

object Mining {
  def candidate = {
    $q(
      "mining/candidate", true, Get, Nil
    )
  }
  def rewardAddress = {
    //curl -X GET "http://127.0.0.1:9052/mining/rewardAddress" -H "accept: application/json" -H "api_key: hello"
    $q(
      "mining/rewardAddress", true, Get, Nil
    )
  }
  def solution(pk:String, w:String, n:String, d:String) = {
    //curl -X POST "http://127.0.0.1:9052/mining/solution" -H "accept: application/json" -H "api_key: hello" -H "Content-Type: application/json" -d "{\"pk\":\"0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5\",\"w\":\"0366ea253123dfdb8d6d9ca2cb9ea98629e8f34015b1e4ba942b1d88badfcc6a12\",\"n\":\"0000000000000000\",\"d\":987654321}"
    $q(
      "mining/solution", true, PostJson, Seq(
        "pk" -> pk,
        "w" -> w,
        "n" -> n,
        "d" -> d
      )
    )
  }
}

object Info {
  def info = $q("info", false, Get, Nil)
}

object Utils {
  def seed = $q("utils/seed", false, Get, Nil, None)

  def address(addr:String) = $q(s"utils/address/$addr", false, Get, Nil, None)

  def seed_length(length:String) = $q(s"utils/seed/$length", false, Get, Nil, None)

  def blake2b(input:String) = $q("utils/hash/blake2b", false, PostJsonRaw, Nil, Some(input))
}

object Transactions {
  // def pushTx(json:String) = $q("peers/all", false, Get, Nil)
  def unconfirmedTx(limit:Int, offset:Int) = {
    val $limit$ = "10"
    $q(
      "transactions/unconfirmed", false, Get,
      Seq("limit" -> limit.toString, "offset" -> offset.toString)
    )
  }

}
