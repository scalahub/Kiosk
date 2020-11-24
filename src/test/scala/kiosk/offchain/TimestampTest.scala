package kiosk.offchain

import kiosk.explorer.Explorer
import kiosk.offchain.parser.Parser

object TimestampTest {
  def main(args: Array[String]): Unit = {
    val str = scala.io.Source.fromFile("src/test/scala/kiosk/offchain/timestamp.json").getLines.mkString
    val str2Protocol = Parser.parse(str)
    val result = new compiler.TxBuilder(new Explorer).compile(str2Protocol)
    println("\nData inputs")
    result.dataInputBoxIds foreach println
    println("\nInputs")
    result.inputBoxIds foreach println
    println("\nOutputs")
    result.outputs foreach println
  }
}
