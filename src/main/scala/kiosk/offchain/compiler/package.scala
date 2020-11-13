package kiosk.offchain

import kiosk.ergo.KioskType
import kiosk.offchain.model.DataType

package object compiler {
  case class DictionaryObject(isUnresolved: Boolean, declaration: Declaration, value: Option[KioskType[_]])

  case class Variable(name: String, `type`: DataType.Type)

  object Height extends Declaration {
    override lazy val maybeId: Option[String] = Some("HEIGHT")
    override lazy val refs: Seq[String] = Nil
    override lazy val refTypes: Seq[DataType.Type] = Nil
    override var `type` = DataType.Int
    override lazy val isLazy = false
    override lazy val maybeValue: Option[String] = None
  }
}
