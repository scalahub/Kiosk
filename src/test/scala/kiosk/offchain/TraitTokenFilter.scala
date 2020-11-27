package kiosk.offchain

import kiosk.offchain.parser.Parser

trait TraitTokenFilter {
  val tokenFilterSource = scala.io.Source.fromFile("src/test/scala/kiosk/offchain/token-filter.json").getLines.mkString
  val tokenFilterProtocol = Parser.parse(tokenFilterSource)
}
