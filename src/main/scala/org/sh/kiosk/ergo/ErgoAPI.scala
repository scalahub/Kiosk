package org.sh.kiosk.ergo

import org.sh.utils.curl.Curl._

case class ReqType(text:String)

object Post extends ReqType(post)
object Get extends ReqType(get)
object PostJson extends ReqType(postJson)
object PostJsonRaw extends ReqType(postJsonRaw)


object ErgoAPI {
  // vars, vals and defs starting with '$' won't appear in HTML
  var $apiKey = "hello"
  var $baseUrl = "http://localhost:9052/"

  var $isMainNet = false

  def setMainNet(isMainNet:Boolean) = {
    val $isMainNet$ = "false"
    $isMainNet = isMainNet
  }
  def setUrl(url:String) = {
    val $url$ = "http://localhost:9052/"
    $baseUrl = url
  }

  def setAPIKey(apiKey:String) = {
    val $apiKey$ = "hello"
    $apiKey = apiKey
  }

  def getUrl = $baseUrl

  def $authHeader = Array(
    ("accept", "application/json"),
    ("api_key", $apiKey),
    ("Content-Type", "application/json")
  )

  val $noauthHeader = Array[(String, String)](
    ("accept", "application/json")
  )

  // q for "query"
  def $q(endPoint: String, isAuth: Boolean, reqType: ReqType, params: Seq[(String, String)], data: Option[String] = None) =
    curl($baseUrl + endPoint, if (isAuth) $authHeader else $noauthHeader, reqType.text, params)(data)
}

