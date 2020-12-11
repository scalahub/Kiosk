package kiosk.offchain.compiler

import kiosk.explorer.Explorer
import kiosk.offchain.compiler.model.Protocol
import kiosk.offchain.reader.Reader

class OnChainLoader(explorer: Explorer)(implicit dictionary: Dictionary) {
  val reader = new Reader(explorer)
  def load(protocol: Protocol) = {
    optSeq(protocol.auxInputUuids).zipWithIndex.foreach { // fetch aux-input boxes from explorer and load into dictionary
      case ((auxInput, uuid), index) =>
        reader
          .getBoxes(auxInput, dictionary.getAuxInputBoxIds)
          .headOption
          .map(dictionary.addAuxInput(_, uuid))
          .getOrElse(throw new Exception(s"No box matched for aux-input at index $index"))
    }
    optSeq(protocol.dataInputUuids).zipWithIndex.foreach { // fetch data-input boxes from explorer and load into dictionary
      case ((dataInput, uuid), index) =>
        reader
          .getBoxes(dataInput, dictionary.getDataInputBoxIds)
          .headOption
          .map(dictionary.addDataInput(_, uuid))
          .getOrElse(throw new Exception(s"No box matched for data-input at index $index"))
    }
    protocol.inputUuids.zipWithIndex.foreach { // fetch input boxes from explorer and load into dictionary
      case ((input, uuid), index) =>
        reader
          .getBoxes(input, dictionary.getInputBoxIds)
          .headOption
          .map(dictionary.addInput(_, uuid))
          .getOrElse(throw new Exception(s"No box matched for input at index $index"))
    }
  }
}
