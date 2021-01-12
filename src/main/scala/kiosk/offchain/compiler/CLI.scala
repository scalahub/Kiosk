package kiosk.offchain.compiler

import kiosk.explorer.Explorer
import kiosk.offchain.parser.Parser._
import org.sh.utils.Util.using
import play.api.libs.json.Json

import scala.io.Source

object CLI {
  def main(args: Array[String]): Unit = {
    if (args.size != 1) println("Usage java -cp <jar> kiosk.offchain.compiler.CLI <script.json>")
    else {
      val script = args(0)
      val source = using(Source.fromFile(script)) { src =>
        src.mkString
      }
      val compileResult = new TxBuilder(new Explorer).compile(parse(source))
      println(Json.toJson(compileResult))
    }
  }
}
