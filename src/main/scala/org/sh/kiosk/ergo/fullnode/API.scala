package org.sh.kiosk.ergo.fullnode

import org.sh.utils.curl.Curl._

case class ReqType(value:String)

object ReqType{
  def fromString(string:String) = {
    allTypes.find(_.value == string).getOrElse(throw new Exception(s"Unknown request type $string"))
  }
  val allTypes = Seq(Post, Get, PostJson, PostJsonRaw)
  object Post extends ReqType(post)
  object Get extends ReqType(get)
  object PostJson extends ReqType(postJson)
  object PostJsonRaw extends ReqType(postJsonRaw)
}

object API {
  import ReqType._
  var nodeApiKey = "hello"
  var nodeBaseUrl = "http://88.198.13.202:9053/"

  private def authHeader = Array(
    ("accept", "application/json"),
    ("api_key", nodeApiKey),
    ("Content-Type", "application/json")
  )

  private val noAuthHeader = Array[(String, String)](
    ("accept", "application/json")
  )

  def nodeInfo = nodeQuery("info", false, Get, Array.empty, Array.empty, None)

  def nodeSetUrl(url: String) = {
    val $url$ = "http://localhost:9052/"
    nodeBaseUrl = url
  }

  def nodeSetAPIKey(newApiKey: String) = {
    val $apiKey$ = "hello"
    this.nodeApiKey = newApiKey
  }

  def nodeQuery(endPoint: String, isAuth: Boolean, reqType: ReqType, paramKeys: Array[String], paramValues: Array[String], data: Option[String]) = {
    val params: Seq[(String, String)] = paramKeys zip paramValues
    curl(nodeBaseUrl + endPoint, if (isAuth) authHeader else noAuthHeader, reqType.value, params)(data)
  }

  // Address related
  def addressGetEccPoint(p2pkAddress: String) =
    API.nodeQuery(s"/utils/addressToRaw/$p2pkAddress", false, Get, Array.empty, Array.empty, None)

  // Peer related
  def peersConnected = nodeQuery("peers/connected", false, Get, Array.empty, Array.empty, None)

  def peersConnect(url:String) = nodeQuery("peers/connect", false, PostJsonRaw, Array.empty, Array.empty, Some(s""""$url""""))

  /*
  def peersBlacklisted = qry("peers/blacklisted", false, Get, Array.empty, Array.empty)
  def peersGetAll = qry("peers/all", false, Get, Array.empty, Array.empty)
  */

  // Wallet related
  def walletInit(pass: String, optional_mnemonicPass: Option[String]) = {
    val optMnemonicKey = optional_mnemonicPass.map(_ => "mnemonicPass")
    nodeQuery("wallet/init",
      true, PostJson,
      Array("pass") ++ optMnemonicKey, Array(pass) ++ optional_mnemonicPass, None
    )
  }

  def walletRestore(pass: String, mnemonic: String, optional_mnemonicPass: Option[String]) = {
    val optMnemonicKey = optional_mnemonicPass.map(_ => "mnemonicPass")
    nodeQuery("wallet/restore",
      true, PostJson,
      Array("pass", "mnemonic") ++ optMnemonicKey, Array(pass, mnemonic) ++ optional_mnemonicPass, None
    )
  }

  def walletUnlock(pass: String) = {
    nodeQuery("wallet/unlock", true, PostJson,
      Array("pass"), Array(pass), None
    )
  }

  def walletLock = nodeQuery("wallet/lock", true, Get, Array.empty, Array.empty, None)


  def walletAddresses = nodeQuery("wallet/addresses", true, Get, Array.empty, Array.empty, None)

  /* // Commented out advanced options below

  def walletDeriveKey(derivationPath:String) = {
    nodeQuery("wallet/deriveKey", true, PostJson, Array("derivationPath"), Array(derivationPath), None)
  }

  def walletDeriveNextKey = {
    nodeQuery("wallet/deriveNextKey", true, Get, Array.empty, Array.empty, None)
  }

  def walletBalances = {
    val $INFO$ = "Get total amount of confirmed Ergo tokens and assets"
    nodeQuery("wallet/balances", true, Get, Array.empty, Array.empty, None)
  }

  def walletTransactions(minInclusionHeight:Int, maxInclusionHeight:Int, minConfirmations:Int, maxConfirmations:Int) = {
    //curl -X GET "http://127.0.0.1:9052/wallet/transactions?minInclusionHeight=10&maxInclusionHeight=100" -H "accept: application/json" -H "api_key: hello"
    require(maxInclusionHeight >= minInclusionHeight, s"maxInclusionHeight ($maxInclusionHeight) must be greater than minInclusionHeight ($minInclusionHeight)")
    require(maxConfirmations > minConfirmations, s"maxConfirmations ($maxConfirmations) must be greater than minConfirmations ($minConfirmations)")
    nodeQuery("wallet/transactions", true, Get,
      Array(
        "minInclusionHeight",
        "maxInclusionHeight",
        "minConfirmations",
        "maxConfirmations"
      ),
      Array(
        minInclusionHeight.toString,
        maxInclusionHeight.toString,
        minConfirmations.toString,
        maxConfirmations.toString
      ),
      None
    )
  }

  def walletBoxes(minConfirmations:Int, minInclusionHeight:Int) = {
    // curl -X GET "http://127.0.0.1:9052/wallet/boxes?minConfirmations=19&minInclusionHeight=21" -H "accept: application/json" -H "api_key: hello"
    nodeQuery("wallet/boxes", true, Get,
      Array(
        "minConfirmations",
        "minInclusionHeight"
      ),
      Array(
        minConfirmations.toString,
        minInclusionHeight.toString
      ), None
    )
  }

  def walletBoxesUnspent(minConfirmations:Int, minInclusionHeight:Int) = {
    // curl -X GET "http://127.0.0.1:9052/wallet/boxes?minConfirmations=19&minInclusionHeight=21" -H "accept: application/json" -H "api_key: hello"
    nodeQuery("wallet/boxes/unspent", true, Get,
      Array(
        "minConfirmations",
        "minInclusionHeight"
      ),
      Array(
        minConfirmations.toString,
        minInclusionHeight.toString
      ), None
    )
  }
  def walletBalancesWithUnconfirmed= nodeQuery("wallet/balances/with_unconfirmed", true, Get, Array.empty, Array.empty, None)
  def walletP2sAddress(source: org.sh.easyweb.Text) =
    nodeQuery("wallet/p2s_address", true, PostJson, Array("source"), Array(source.getText), None)
  */

  // Mining related
  def miningCandidate = {
    nodeQuery(
      "mining/candidate", true, Get, Array.empty, Array.empty, None
    )
  }
  def miningRewardAddress = {
    //curl -X GET "http://127.0.0.1:9052/mining/rewardAddress" -H "accept: application/json" -H "api_key: hello"
    nodeQuery(
      "mining/rewardAddress", true, Get, Array.empty, Array.empty, None
    )
  }
  def miningSolution(pk:String, w:String, n:String, d:String) = {
    //curl -X POST "http://127.0.0.1:9052/mining/solution" -H "accept: application/json" -H "api_key: hello" -H "Content-Type: application/json" -d "{\"pk\":\"0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5\",\"w\":\"0366ea253123dfdb8d6d9ca2cb9ea98629e8f34015b1e4ba942b1d88badfcc6a12\",\"n\":\"0000000000000000\",\"d\":987654321}"
    nodeQuery(
      "mining/solution", true, PostJson,
      Array("pk", "w", "n", "d"),
      Array(pk, w, n, d),
      None
    )
  }

  // Transaction related
  def transactionsUnconfirmed(limit:Int, offset:Int) = {
    val $limit$ = "10"
    nodeQuery(
      "transactions/unconfirmed", false, Get,
      Array("limit", "offset"),
      Array(limit.toString, offset.toString),
      None
    )
  }

  // Utils
  def utilsSeed = {
    val $INFO$ = "Generates a random seed"
    nodeQuery("utils/seed", false, Get, Array.empty, Array.empty, None)
  }

  def utilsAddress(addr:String) = {
    val $INFO$ = "Checks if an address is valid"
    nodeQuery(s"utils/address/$addr", false, Get, Array.empty, Array.empty, None)
  }

  def utilsSeedLength(length:String) = {
    val $INFO$ = "Generates a random seed of given length"
    nodeQuery(s"utils/seed/$length", false, Get, Array.empty, Array.empty, None)
  }

  def utilsBlake2b(input:String) = nodeQuery("utils/hash/blake2b", false, PostJsonRaw, Array.empty, Array.empty, Some(s""""$input""""))

}

