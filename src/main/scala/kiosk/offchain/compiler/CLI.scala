package kiosk.offchain.compiler

import kiosk.offchain.compiler
import kiosk.offchain.parser.Parser._
import play.api.libs.json.Json

import scala.io.Source

object CLI {
  def main(args: Array[String]): Unit = {
    if (args.size != 1) println("Usage java -cp <jar> kiosk.offchain.compiler.CLI <script.json>")
    else {
      val script = args(0)
      val source = Source.fromFile(script).mkString
      val compileResult = compiler.Compiler.compile(parse(source))
      println(Json.toJson(compileResult))
    }
  }
}