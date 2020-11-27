package kiosk.offchain

import kiosk.offchain.parser.Parser

trait TraitTimestamp {
  val timestampSource = scala.io.Source.fromFile("src/test/scala/kiosk/offchain/timestamp.json").getLines.mkString
  val timestampProtocol = Parser.parse(timestampSource)
}
