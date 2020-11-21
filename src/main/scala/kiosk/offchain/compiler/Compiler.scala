package kiosk.offchain.compiler

import kiosk.offchain.model._

object Compiler {
  def compile(protocol: Protocol) = {
    val dictionary = new Dictionary
    // Step 1. validate that constants are properly encoded
    optSeq(protocol.constants).map(_.getValue(dictionary))
    // Step 2. load declarations (also does semantic validation)
    Loader.load(protocol)(dictionary)
    // Step 3. load on-chain declarations
    OnChainLoader.load(protocol)(dictionary)
    // Step 4. build outputs
    val outputs = Builder.buildOutputs(protocol)(dictionary)
    // Printer.print(dictionary) // print values

    CompileResults(dictionary.getDataInputBoxIds, dictionary.getInputBoxIds, dictionary.getInputNanoErgs, dictionary.getInputTokens, outputs, protocol.fee)
  }
}
