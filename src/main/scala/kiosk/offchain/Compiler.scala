package kiosk.offchain

import play.api.libs.json.Json

object Compiler {
  def compile(string: String) = {
    implicit val dictionary = new Dictionary
    val parser = new Parser
    import parser._
    val protocol = Json.parse(string).as[Protocol]
    dictionary.print
    Json.toJson(protocol).toString()
  }
}
