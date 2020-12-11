package kiosk.offchain.compiler

import kiosk.explorer.Explorer
import kiosk.offchain.compiler.model._

class TxBuilder(explorer: Explorer) {
  def compile(protocol: Protocol) = {
    implicit val dictionary = new Dictionary(explorer.getHeight)
    // Step 1. validate that constants are properly encoded
    optSeq(protocol.constants).map(_.getValue)
    // Step 2. load declarations (also does semantic validation)
    (new OffChainLoader).load(protocol)
    // Step 3. load on-chain declarations
    new OnChainLoader(explorer).load(protocol)
    // Step 4. build outputs
    val outputs = (new Builder).buildOutputs(protocol)
    // Return final result
    CompileResult(
      dictionary.getDataInputBoxIds,
      dictionary.getInputBoxIds,
      dictionary.getInputNanoErgs,
      dictionary.getInputTokens,
      outputs,
      protocol.fee
    )
  }
}
