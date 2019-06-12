package org.sh.kiosk.ergo

import org.sh.easyweb.Text
import org.sh.kiosk.ergo.ErgoAPI._

object ErgoAdmin extends App {
  new org.sh.easyweb.AutoWeb(List(ErgoScript, ErgoAPI, Peers, Wallet, Utils), "ErgoDemo")
}

object Peers {
  // vars, vals and defs starting with '$' won't appear in HTML
  def all = $q("peers/all", false, Get, Nil)

  def blacklisted = $q("peers/blacklisted", false, Get, Nil)

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

  def deriveKey(derivationPath:String) = {
    $q("wallet/deriveKey", true, PostJson, Seq("derivationPath" -> derivationPath))
  }

  def deriveNextKey = {
    $q("wallet/deriveNextKey", true, Get, Nil)
  }

  def lock = $q("wallet/lock", true, Get, Nil)

  def confBalances = $q("wallet/balances", true, Get, Nil)

  def allBalances = $q("wallet/balances/with_unconfirmed", true, Get, Nil)

  def addresses = $q("wallet/addresses", true, Get, Nil)

  def p2s_address(source: Text) =
    $q("wallet/p2s_address", true, PostJson, Seq("source" -> source.getText))

  def p2sh_address(source: Text) =
    $q("wallet/p2sh_address", true, PostJson, Seq("source" -> source.getText))
}

object Utils {
  def info = $q("info", false, Get, Nil)

  def blake2b(input:String) = $q("utils/hash/blake2b", false, PostJsonRaw, Nil, Some(input))

  def seed = $q("utils/seed", false, Get, Nil, None)

  def seed_length(length:String) = $q(s"utils/seed/$length", false, Get, Nil, None)

  def compile(ergoScript:Text) = ???

  // def pushTx(json:String) = $q("peers/all", false, Get, Nil)
  def unconfirmedTx(limit:Int, offset:Int) = {
    val $limit$ = "10"
    $q(
      "transactions/unconfirmed", false, Get,
      Seq("limit" -> limit.toString, "offset" -> offset.toString)
    )
  }

}
