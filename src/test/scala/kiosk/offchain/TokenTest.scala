package kiosk.offchain

import kiosk.offchain.compiler.{Dictionary, Loader, optSeq}
import kiosk.offchain.parser.Parser

object TokenTest {
  def main(args: Array[String]): Unit = {
    val str = scala.io.Source.fromFile("src/test/scala/kiosk/offchain/token.json").getLines.mkString
    val protocol = Parser.parse(str)
    implicit val dictionary = new Dictionary
    // Step 1. validate that constants are properly encoded
    optSeq(protocol.constants).map(_.getValue) foreach println
    // Step 2. load declarations (also does semantic validation)
    (new Loader).load(protocol)
  }
}
