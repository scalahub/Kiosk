package kiosk.offchain.compiler

import kiosk.offchain.model.Protocol
import kiosk.offchain.reader.Reader

object OnChainLoader {
  def load(protocol: Protocol)(dictionary: Dictionary) = { // fetch data-input boxes from explorer and load into dictionary
    optSeq(protocol.dataInputs).zipWithIndex.foreach {
      case (dataInput, index) =>
        Reader
          .getBox(dataInput, dictionary.getDataInputBoxIds)(dictionary)
          .map(dictionary.addDataInput)
          .getOrElse(throw new Exception(s"No box matched for data-input at index $index"))
    }
    protocol.inputs.zipWithIndex.foreach {
      case (input, index) =>
        Reader
          .getBox(input, dictionary.getInputBoxIds)(dictionary)
          .map(dictionary.addInput)
          .getOrElse(throw new Exception(s"No box matched for input at index $index"))
    }
  }
}
