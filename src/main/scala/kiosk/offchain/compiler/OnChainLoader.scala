package kiosk.offchain.compiler

import kiosk.explorer.Explorer
import kiosk.offchain.compiler.model.Protocol
import kiosk.offchain.reader.Reader

class OnChainLoader(explorer: Explorer)(implicit dictionary: Dictionary) {
  val reader = new Reader(explorer)
  def load(protocol: Protocol) = {
    optSeq(protocol.dataInputs).zipWithIndex.foreach { // fetch data-input boxes from explorer and load into dictionary
      case (dataInput, index) =>
        reader
          .getBoxes(dataInput, dictionary.getDataInputBoxIds)
          .headOption
          .map(dictionary.addDataInput)
          .getOrElse(throw new Exception(s"No box matched for data-input at index $index"))
    }
    protocol.inputs.zipWithIndex.foreach { // fetch input boxes from explorer and load into dictionary
      case (input, index) =>
        reader
          .getBoxes(input, dictionary.getInputBoxIds)
          .headOption
          .map(dictionary.addInput)
          .getOrElse(throw new Exception(s"No box matched for input at index $index"))
    }
  }
}
