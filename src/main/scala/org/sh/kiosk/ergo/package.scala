package org.sh.kiosk

import org.json.JSONObject
import org.sh.utils.json.JSONUtil.JsonFormatted
import org.sh.cryptonode.util.BytesUtil._

package object ergo {
  type Register = Array[Byte]
  type Registers = Array[Register]
  type ID = Array[Byte]
  type Amount = Long

  type Token = (ID, Amount)
  type Tokens = Array[Token]

  def regs2Json(registers: Registers) = {
    var ctr = 4
    registers.map{register =>
      val jo = new JSONObject()
      val name = s"R$ctr"
      jo.put(name, register.encodeHex)
      ctr += 1
      jo
    }
  }

  def tokens2Json(tokens: Tokens) = {
    var ctr = 0
    tokens.map{token =>
      val jo = new JSONObject()
      val (id, amount) = token
      jo.put("index", ctr)
      jo.put("id", id)
      jo.put("amount", amount)
      ctr += 1
      jo
    }
  }

  case class Box(address:String, value:Long, registers: Registers, tokens: Tokens) extends JsonFormatted {
    val keys = Array[String]("address", "value", "registers", "tokens")
    val vals = Array[Any](address, value, regs2Json(registers), tokens2Json(tokens))
  }

}
