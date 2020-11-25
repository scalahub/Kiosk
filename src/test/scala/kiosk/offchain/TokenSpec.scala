package kiosk.offchain

import kiosk.offchain.parser.Parser

trait TokenSpec {
  val tokenSource = scala.io.Source.fromFile("src/test/scala/kiosk/offchain/token.json").getLines.mkString
  val tokenProtocol = Parser.parse(tokenSource)
}
