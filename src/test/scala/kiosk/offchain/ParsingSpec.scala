package kiosk.offchain

import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo.{KioskErgoTree, KioskGroupElement}
import kiosk.offchain.compiler.model.BinaryOperator._
import kiosk.offchain.compiler.model.{BinaryOp, Constant, DataType}
import kiosk.offchain.compiler.{Dictionary, optSeq}
import kiosk.offchain.parser.Parser
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito._
import play.api.libs.json.JsValue

class ParsingSpec extends WordSpec with MockitoSugar with Matchers with TraitTokenFilter with TraitTimestamp with TraitDummyProtocol {
  "Protocol parser" should {
    "parse constants from token-filter.json correctly" in {
      val constants = tokenProtocol.constants.get
      constants(0) shouldEqual Constant(
        name = "506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7",
        `type` = DataType.CollByte,
        value = "506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7"
      )
      constants(1) shouldEqual Constant(
        name = "ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f",
        `type` = DataType.CollByte,
        value = "ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f"
      )
      constants(2) shouldEqual Constant(
        name = "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea",
        `type` = DataType.CollByte,
        value = "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"
      )
      constants(3) shouldEqual Constant(name = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK", DataType.Address, value = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")
      constants(4) shouldEqual Constant(name = "1234", DataType.Long, value = "1234")
      constants(5) shouldEqual Constant(name = "1", DataType.Long, value = "1")
    }

    "parse operations from token-filter.json correctly" in {
      val binaryOps = tokenProtocol.binaryOps.get
      binaryOps(0) shouldEqual BinaryOp(name = "myTokenAmount+1234", first = "myTokenAmount", Add, second = "1234")
    }

    "parse constants from timestamp.json correctly" in {
      val constants = timestampProtocol.constants.get
      constants(0) shouldEqual Constant(name = "myBoxId", `type` = DataType.CollByte, value = "506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7")
      constants(1) shouldEqual Constant(
        name = "emissionAddress",
        `type` = DataType.Address,
        value =
          "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi"
      )
      constants(2) shouldEqual Constant(name = "timestampAddress", `type` = DataType.Address, value = "4MQyMKvMbnCJG3aJ")
      constants(3) shouldEqual Constant(name = "myTokenId", `type` = DataType.CollByte, value = "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")
      constants(4) shouldEqual Constant(name = "minTokenAmount", `type` = DataType.Long, value = "2")
      constants(5) shouldEqual Constant(name = "one", DataType.Long, value = "1")
      constants(6) shouldEqual Constant(name = "minStorageRent", DataType.Long, value = "2000000")
    }

    "parse operations from timestamp.json correctly" in {
      val binaryOps = timestampProtocol.binaryOps.get
      binaryOps(0) shouldEqual BinaryOp(name = "balanceTokenAmount", first = "inputTokenAmount", Sub, second = "one")
    }

    "parse and un-parse correctly" in {
      val protocolToJson: JsValue = Parser.unparse(dummyProtocol) // convert to json
      val protocolToJsonToProtocol = Parser.parse(protocolToJson.toString) // convert back to protocol
      protocolToJsonToProtocol shouldBe dummyProtocol
    }

    "parse protocol from dummy.json correctly" in {
      val str = scala.io.Source.fromFile("src/test/scala/kiosk/offchain/dummy.json").getLines.mkString
      val strToProtocol = Parser.parse(str)
      strToProtocol shouldBe dummyProtocol
    }

    "parse constants from dummy.json correctly" in {
      val str = scala.io.Source.fromFile("src/test/scala/kiosk/offchain/dummy.json").getLines.mkString
      val strToProtocol = Parser.parse(str)
      implicit val dictionary = new Dictionary(12345)
      val constants = optSeq(strToProtocol.constants)
      constants(0) shouldEqual Constant("myLong1", DataType.Long, "1234")
      constants(1) shouldEqual Constant("myCollByte", DataType.CollByte, "506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7")
      constants(2) shouldEqual Constant("myInt", DataType.Int, "1234")
      constants(3) shouldEqual Constant("myTokenId", DataType.CollByte, "ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f")
      constants(4) shouldEqual Constant("myGroupElement", DataType.GroupElement, "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67")
      constants(5) shouldEqual Constant("myErgoTree1", DataType.ErgoTree, "10010101D17300")
      constants(6) shouldEqual Constant("myAddress", DataType.Address, "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")

      val values = constants.map(_.getValue)
      values(0).value shouldEqual 1234L
      values(1).toString shouldEqual "506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7"
      values(2).value shouldEqual 1234
      values(3).toString shouldEqual "ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f"
      values(4) shouldEqual KioskGroupElement(ScalaErgoConverters.stringToGroupElement("028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"))
      values(5) shouldEqual KioskErgoTree(ScalaErgoConverters.stringToErgoTree("10010101D17300"))
      values(6) shouldEqual KioskErgoTree(ScalaErgoConverters.getAddressFromString("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK").script)
    }
  }
}
