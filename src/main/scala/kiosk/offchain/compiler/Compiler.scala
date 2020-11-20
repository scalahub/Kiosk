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

    val dataInputs = dictionary.getOnChainDataInputs.map(_.toKioskBox)
    val inputs = dictionary.getOnChainInputs.map(_.toKioskBox)
    CompileResults(
      dataInputs.map(_.optBoxId.get),
      inputs.map(_.optBoxId.get),
      inputs.map(_.value).sum,
      inputs.flatMap(_.tokens).groupBy(_._1).map { case (id, seq) => (id, seq.map(_._2).sum) }.toSeq,
      outputs,
      protocol.fee
    )
  }
}
