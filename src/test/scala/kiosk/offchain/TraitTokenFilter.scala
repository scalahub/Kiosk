package kiosk.offchain

import kiosk.offchain.parser.Parser

trait TraitTokenFilter {
  val tokenSource = scala.io.Source.fromFile("src/test/scala/kiosk/offchain/token-filter.json").getLines.mkString
  val tokenProtocol = Parser.parse(tokenSource)
}
