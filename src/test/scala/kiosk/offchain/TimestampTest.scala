package kiosk.offchain

import kiosk.offchain.parser.Parser

object TimestampTest {
  val timestampScript =
    """{
      |  "constants": [
      |    {
      |      "name": "myBoxId",
      |      "type": "CollByte",
      |      "value": "ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f"
      |    },
      |    {
      |      "name": "emissionAddress",
      |      "type": "Address",
      |      "value": "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi"
      |    },
      |    {
      |      "name": "timestampAddress",
      |      "type": "Address",
      |      "value": "4MQyMKvMbnCJG3aJ"
      |    },
      |    {
      |      "name": "myTokenId",
      |      "type": "CollByte",
      |      "value": "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"
      |    },
      |    {
      |      "name": "minTokenAmount",
      |      "type": "Long",
      |      "value": "2"
      |    },
      |    {
      |      "name": "one",
      |      "type": "Long",
      |      "value": "1"
      |    },
      |    {
      |      "name": "minStorageRent",
      |      "type": "Long",
      |      "value": "2000000"
      |    }
      |  ],
      |  "dataInputs": [
      |    {
      |      "id": {
      |        "value": "myBoxId"
      |      }
      |    }
      |  ],
      |  "inputs": [
      |    {
      |      "address": {
      |        "value": "emissionAddress"
      |      },
      |      "tokens": [
      |        {
      |          "index": 0,
      |          "id": {
      |            "value": "myTokenId"
      |          },
      |          "amount": {
      |            "name": "inputTokenAmount",
      |            "value": "minTokenAmount",
      |            "filter": "Ge"
      |          }
      |        }
      |      ],
      |      "nanoErgs": {
      |        "name": "inputNanoErgs"
      |      }
      |    }
      |  ],
      |  "outputs": [
      |    {
      |      "address": {
      |        "value": "emissionAddress"
      |      },
      |      "tokens": [
      |        {
      |          "index": 0,
      |          "id": {
      |            "value": "myTokenId"
      |          },
      |          "amount": {
      |            "value": "balanceTokenAmount"
      |          }
      |        }
      |      ],
      |      "nanoErgs": {
      |        "value": "inputNanoErgs"
      |      }
      |    },
      |    {
      |      "address": {
      |        "value": "timestampAddress"
      |      },
      |      "registers": [
      |        {
      |          "value": "myBoxId",
      |          "num": "R4",
      |          "type": "CollByte"
      |        },
      |        {
      |          "value": "HEIGHT",
      |          "num": "R5",
      |          "type": "Int"
      |        }
      |      ],
      |      "tokens": [
      |        {
      |          "index": 0,
      |          "id": {
      |            "value": "myTokenId"
      |          },
      |          "amount": {
      |            "value": "one"
      |          }
      |        }
      |      ],
      |      "nanoErgs": {
      |        "value": "minStorageRent"
      |      }
      |    }
      |  ],
      |  "binaryOps": [
      |    {
      |      "name": "balanceTokenAmount",
      |      "first": "inputTokenAmount",
      |      "op": "Sub",
      |      "second": "one"
      |    }
      |  ]
      |}
      |""".stripMargin
  def main(args: Array[String]): Unit = {
    val str2Protocol = Parser.parse(timestampScript)
    val result = compiler.Compiler.compile(str2Protocol)
    println("\nData inputs")
    result.dataInputBoxIds foreach println
    println("\nInputs")
    result.inputBoxIds foreach println
    println("\nOutputs")
    result.outputs foreach println
  }
}
