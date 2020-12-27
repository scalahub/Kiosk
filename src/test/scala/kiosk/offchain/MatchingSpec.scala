package kiosk.offchain

import kiosk.ergo.{KioskBox, KioskInt}
import kiosk.explorer.Explorer
import kiosk.offchain.compiler.{TxBuilder, model}
import kiosk.offchain.compiler.model.{MatchingOptions, Output}
import kiosk.offchain.compiler.model.MatchingOptions.Strict
import org.mockito.Mockito.when
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito._
import play.api.libs.json.JsResultException

class MatchingSpec extends WordSpec with MockitoSugar with Matchers with TraitTokenFilter with TraitTimestamp {
  val explorer = mock[Explorer]
  when(explorer.getHeight) thenReturn 12345
  val txBuilder = new TxBuilder(explorer)
  trait TokenMocks {
    val fakeBox0ExactTokens = KioskBox(
      address = "9gMUzFpsjZeHFMgzwjc3TNecZ3WJ2uz2Wfqh4SkxJqMEQrTNitB",
      value = 1000000000L,
      registers = Array(),
      tokens = Array(
        ("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f", 10000),
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 123)
      ),
      optBoxId = Some("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"),
      spentTxId = None
    )

    val fakeBox0ExtraTokens = KioskBox( // extra tokens
      address = "9gMUzFpsjZeHFMgzwjc3TNecZ3WJ2uz2Wfqh4SkxJqMEQrTNitB",
      value = 1000000000L,
      registers = Array(),
      tokens = Array(
        ("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f", 10000),
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 123),
        ("5c674366216d127f7424bfcf1bf52310f9c34cd8d07013c804a95bb8ce9e4f82", 1)
      ),
      optBoxId = Some("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"),
      spentTxId = None
    )

    val fakeBox1ExactTokens = KioskBox(
      address = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK",
      value = 2000000000L,
      registers = Array(),
      tokens = Array(
        ("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 1235),
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 123),
        ("490ef5a88d33d7b3eb7b16d4062ee9c3e204f9e6123f4bd6d97156a5b05b592a", 12),
        ("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f", 1),
      ),
      optBoxId = Some("af0e35e1cf5a8890d70cef498c996dcd3e7658cfadd37695425032d4f8327d8a"),
      spentTxId = None
    )

    val fakeBox1ExactTokensWrongAmount = KioskBox(
      address = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK",
      value = 2000000000L,
      registers = Array(),
      tokens = Array(
        ("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 1233),
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 123),
        ("490ef5a88d33d7b3eb7b16d4062ee9c3e204f9e6123f4bd6d97156a5b05b592a", 12),
        ("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f", 1),
      ),
      optBoxId = Some("af0e35e1cf5a8890d70cef498c996dcd3e7658cfadd37695425032d4f8327d8a"),
      spentTxId = None
    )

    val fakeBox1ExtraTokens = KioskBox( // extra tokens
      address = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK",
      value = 2000000000L,
      registers = Array(),
      tokens = Array(
        ("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 1235),
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 123),
        ("490ef5a88d33d7b3eb7b16d4062ee9c3e204f9e6123f4bd6d97156a5b05b592a", 12),
        ("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f", 1),
        ("e3e335a1d34ec7ad4eecde3813a4b066114692cad65b3aa0f3876abba8bb6307", 2)
      ),
      optBoxId = Some("af0e35e1cf5a8890d70cef498c996dcd3e7658cfadd37695425032d4f8327d8a"),
      spentTxId = None
    )

    val fakeBox2LessTokens = KioskBox(
      address = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK",
      value = 4000000000L,
      registers = Array(),
      tokens = Array(
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 10),
        ("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f", 1),
        ("e3e335a1d34ec7ad4eecde3813a4b066114692cad65b3aa0f3876abba8bb6307", 2)
      ),
      optBoxId = Some("879e437e94668ee11f9f4575fc623e420bc04a466fffca31ae0623ce950a861d"),
      spentTxId = None
    )

    val fakeBox2ExactTokens = KioskBox(
      address = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK",
      value = 4000000000L,
      registers = Array(),
      tokens = Array(
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 10),
        ("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f", 1),
        ("e3e335a1d34ec7ad4eecde3813a4b066114692cad65b3aa0f3876abba8bb6307", 2)
      ),
      optBoxId = Some("879e437e94668ee11f9f4575fc623e420bc04a466fffca31ae0623ce950a861d"),
      spentTxId = None
    )
  }
  trait TimestampMocks {
    val fakeDataInputBox = KioskBox(
      address = "9gMUzFpsjZeHFMgzwjc3TNecZ3WJ2uz2Wfqh4SkxJqMEQrTNitB",
      value = 1000000000L,
      registers = Array(),
      tokens = Array(
        ("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f", 10000),
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 123)
      ),
      optBoxId = Some("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7"),
      spentTxId = None
    )

    val fakeEmissionBoxExtraTokens = KioskBox( // extra tokens
      address =
        "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi",
      value = 1100000L,
      registers = Array(),
      tokens = Array(
        ("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 100),
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 123),
        ("5c674366216d127f7424bfcf1bf52310f9c34cd8d07013c804a95bb8ce9e4f82", 1)
      ),
      optBoxId = Some("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"),
      spentTxId = None
    )

    val fakeEmissionBoxLessTokens = KioskBox(
      address =
        "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi",
      value = 2200000L,
      registers = Array(),
      tokens = Array(
        ("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 1),
      ),
      optBoxId = Some("af0e35e1cf5a8890d70cef498c996dcd3e7658cfadd37695425032d4f8327d8a"),
      spentTxId = None
    )

    val fakeEmissionBoxExactTokens = KioskBox(
      address =
        "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi",
      value = 3300000L,
      registers = Array(),
      tokens = Array(
        ("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 22),
      ),
      optBoxId = Some("43b0c3add1fde20244a3467798a777684f9234d1f56f31ad01a297c86c6d40c7"),
      spentTxId = None
    )
  }

  "Compilation for token-filter.json" should {
    "select matched boxes" in new TokenMocks {
      tokenFilterProtocol.inputs.size shouldBe 2
      tokenFilterProtocol.inputs(0).options shouldBe Some(Set(Strict))
      tokenFilterProtocol.inputs(1).options shouldBe Some(Set(Strict))
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox1ExactTokens, fakeBox2LessTokens)
      val result = new compiler.TxBuilder(explorer).compile(tokenFilterProtocol)
      result.inputBoxIds shouldBe Seq("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", "af0e35e1cf5a8890d70cef498c996dcd3e7658cfadd37695425032d4f8327d8a")
    }

    "select matched boxes in any order" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox2LessTokens, fakeBox1ExactTokens)
      val result = new compiler.TxBuilder(explorer).compile(tokenFilterProtocol)
      result.inputBoxIds shouldBe Seq("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", "af0e35e1cf5a8890d70cef498c996dcd3e7658cfadd37695425032d4f8327d8a")
    }

    "reject if invalid amount in non-Optional input" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox2LessTokens, fakeBox1ExactTokensWrongAmount)
      the[Exception] thrownBy new compiler.TxBuilder(explorer)
        .compile(tokenFilterProtocol.copy(inputs = Seq(tokenFilterProtocol.inputs(0), tokenFilterProtocol.inputs(1).copy(options = None)))) should have message "No box matched for input at index 1"
    }

    "accept if invalid amount in Optional input" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox2LessTokens, fakeBox1ExactTokensWrongAmount)
      val result = new compiler.TxBuilder(explorer)
        .compile(
          tokenFilterProtocol.copy(
            inputs = Seq(tokenFilterProtocol.inputs(0), tokenFilterProtocol.inputs(1).copy(options = Some(Set(MatchingOptions.Optional))))
          )
        )
      result.inputBoxIds shouldBe Seq("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")
    }

    "reject if invalid amount in Optional input containing a target used in a non-Optional output" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox2LessTokens, fakeBox1ExactTokensWrongAmount)
      the[Exception] thrownBy new compiler.TxBuilder(explorer)
        .compile(
          tokenFilterProtocol.copy(
            inputs = Seq(tokenFilterProtocol.inputs(0), tokenFilterProtocol.inputs(1).copy(options = Some(Set(MatchingOptions.Optional)))),
            outputs = Seq(
              Output(
                address = model.Address(name = None, value = Some("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"), values = None),
                registers = None,
                tokens = None,
                nanoErgs = model.Long(name = None, value = Some("thirdTokenAmount"), filter = None),
                options = None
              )
            )
          )
        ) should have message "Output declaration generated zero boxes (use 'Optional' flag to prevent this error): Output(unnamed: Address,None,None,unnamed: Long,None)"
    }

    "accept if invalid amount in Optional input containing a target used in an Optional output" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox2LessTokens, fakeBox1ExactTokensWrongAmount)
      val result = new compiler.TxBuilder(explorer)
        .compile(
          tokenFilterProtocol.copy(
            inputs = Seq(tokenFilterProtocol.inputs(0), tokenFilterProtocol.inputs(1).copy(options = Some(Set(MatchingOptions.Optional)))),
            outputs = Seq(
              Output(
                address = model.Address(name = None, value = Some("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"), values = None),
                registers = None,
                tokens = None,
                nanoErgs = model.Long(name = None, value = Some("thirdTokenAmount"), filter = None),
                options = Some(Set(MatchingOptions.Optional))
              )
            )
          )
        )
      result.inputBoxIds shouldBe Seq("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")
      result.outputs shouldBe Nil
    }

    // ToDo: Add more tests for 'Multi' option in outputs

    "reject boxes with extra tokens and Strict(0)" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExtraTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox1ExactTokens, fakeBox2LessTokens)
      the[Exception] thrownBy new compiler.TxBuilder(explorer).compile(tokenFilterProtocol) should have message "No box matched for input at index 0"
    }

    "select boxes with extra tokens and no Strict(0)" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExtraTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox1ExactTokens, fakeBox2LessTokens)
      val result = new compiler.TxBuilder(explorer).compile(tokenFilterProtocol.copy(inputs = Seq(tokenFilterProtocol.inputs(0).copy(options = None), tokenFilterProtocol.inputs(1))))
      result.inputBoxIds shouldBe Seq("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", "af0e35e1cf5a8890d70cef498c996dcd3e7658cfadd37695425032d4f8327d8a")
    }

    "reject boxes with extra tokens and Strict(1)" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox2LessTokens, fakeBox1ExtraTokens)
      the[Exception] thrownBy new compiler.TxBuilder(explorer).compile(tokenFilterProtocol) should have message "No box matched for input at index 1"
    }

    "select boxes with extra tokens and no Strict(1)" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox2LessTokens, fakeBox1ExtraTokens)
      val result = new compiler.TxBuilder(explorer).compile(tokenFilterProtocol.copy(inputs = Seq(tokenFilterProtocol.inputs(0), tokenFilterProtocol.inputs(1).copy(options = None))))
      result.inputBoxIds shouldBe Seq("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", "af0e35e1cf5a8890d70cef498c996dcd3e7658cfadd37695425032d4f8327d8a")
    }
  }

  "Compilation for timestamp.json" should {
    "select matched boxes" in new TimestampMocks {
      timestampProtocol.inputs.size shouldBe 1
      timestampProtocol.inputs(0).options shouldBe Some(Set(Strict))

      when(explorer.getBoxById("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7")) thenReturn fakeDataInputBox
      when(explorer.getUnspentBoxes(
        "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi")) thenReturn Seq(
        fakeEmissionBoxLessTokens,
        fakeEmissionBoxExtraTokens,
        fakeEmissionBoxExactTokens)

      val result = new TxBuilder(explorer).compile(timestampProtocol)

      result.dataInputBoxIds shouldBe Seq("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7")
      result.inputBoxIds shouldBe Seq("43b0c3add1fde20244a3467798a777684f9234d1f56f31ad01a297c86c6d40c7")
      val outputs = result.outputs
      outputs(0).address shouldBe "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi"
      outputs(0).tokens(0)._1 shouldBe "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"
      outputs(0).tokens(0)._2 shouldBe 21
      outputs(0).value shouldBe 3300000

      outputs(1).address shouldBe "4MQyMKvMbnCJG3aJ"
      outputs(1).tokens(0)._1 shouldBe "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"
      outputs(1).tokens(0)._2 shouldBe 1
      outputs(1).value shouldBe 2000000
      outputs(1).registers(0).hex shouldBe "0e20506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7"
      outputs(1).registers(0).toString shouldBe "506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7"
      outputs(1).registers(1).asInstanceOf[KioskInt].value shouldBe 12345
      outputs(1).registers(1).hex shouldBe "04f2c001"
    }

    "reject with no matched inputs" in new TimestampMocks {
      timestampProtocol.inputs.size shouldBe 1
      timestampProtocol.inputs(0).options shouldBe Some(Set(Strict))

      when(explorer.getBoxById("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7")) thenReturn fakeDataInputBox
      when(explorer.getUnspentBoxes(
        "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi")) thenReturn Seq(
        fakeEmissionBoxLessTokens,
        fakeEmissionBoxExtraTokens)

      the[Exception] thrownBy txBuilder.compile(timestampProtocol) should have message "No box matched for input at index 0"
    }

    "reject with no matched data inputs" in new TimestampMocks {
      timestampProtocol.inputs.size shouldBe 1
      timestampProtocol.inputs(0).options shouldBe Some(Set(Strict))

      when(explorer.getBoxById("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7")) thenThrow new JsResultException(Nil)

      the[Exception] thrownBy txBuilder.compile(timestampProtocol) should have message "JsResultException(errors:List())"
    }
  }
}
