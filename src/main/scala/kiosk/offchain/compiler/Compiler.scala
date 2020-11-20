package kiosk.offchain.compiler

import kiosk.offchain.model._

object Compiler {
  def compile(protocol: Protocol) = {
    val dictionary = new Dictionary
    // validate that constants are properly encoded
    optSeq(protocol.constants).map(value => value.getValue(dictionary))
    // load declarations and valid then semantically
    Loader.load(protocol)(dictionary)
    // load on-chain declarations
    OnChainLoader.load(protocol)(dictionary)
    // create outputs
    val outputs = Builder.buildOutputs(protocol)(dictionary)
    outputs foreach println
    // print values
    // Printer.print(dictionary)
  }
}
