package kiosk.explorer

import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo.{KioskBox, KioskType, Token}

object Reader {

  import io.circe.Json

  private val baseUrl = "https://api.ergoplatform.com"
  private val boxUrl = s"$baseUrl/transactions/boxes/"

  def getBoxById(boxId:String) = {
    getBoxFromJson(Curl.get(boxUrl + boxId))
  }

  private def getBoxFromJson(j: Json):KioskBox = {
    val value = (j \\ "value").map(v => v.asNumber.get).apply(0)
    val assets: Array[Json] = (j \\ "assets").map(v => v.asArray.get).apply(0).toArray
    val tokens: Array[Token] = assets.map{ asset =>
      val tokenID = (asset \\ "tokenId").map(v => v.asString.get).apply(0)
      val value = (asset \\ "amount").map(v => v.asNumber.get).apply(0).toLong.get
      (tokenID, value)
    }
    val registers: Array[String] = (j \\ "additionalRegisters").flatMap{ r =>
      r.asObject.get.toList.map{
        case (key, value) => (key -> value.asString.get)
      }
    }.sortBy(_._1).map(_._2).toArray

    val address = (j \\ "address").map(v => v.asString.get).apply(0)

    val regs:Array[KioskType[_]] = registers.map(ScalaErgoConverters.deserialize)

    KioskBox(address, value.toLong.get, regs, tokens)
  }

}
