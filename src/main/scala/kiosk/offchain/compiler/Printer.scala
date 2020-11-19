package kiosk.offchain.compiler

import java.util.UUID

import kiosk.ergo._

import scala.util.Try

object Printer {
  def printObject(key: String, dictionaryObject: DictionaryObject) = println(s"$key: ${dictionaryObject.declaration.`type`}")

  def printObjects(objects: Seq[(String, DictionaryObject)]) = objects.map { case (key, dictionaryObject) => printObject(key, dictionaryObject) }

  def getValueToPrint(value: KioskType[_]) = {
    value match {
      case _: KioskErgoTree        => "<ErgoTree>"
      case long: KioskLong         => long.value
      case int: KioskInt           => int.value
      case collByte: KioskCollByte => collByte.value.toArray.encodeHex
      case any: KioskType[_]       => any.hex
    }
  }
  def printValues(objects: Seq[(String, DictionaryObject)], dictionary: Dictionary) = objects.foreach {
    case (name, value) => if (Try(UUID.fromString(name)).isFailure) println(s"${value.declaration} = ${getValueToPrint(value.declaration.getValue(dictionary))}")
  }

  def print(dictionary: Dictionary) = {
    val used: Seq[(String, DictionaryObject)] = dictionary.getDictionaryObjects(unresolved = false)
    val unused: Seq[(String, DictionaryObject)] = dictionary.getDictionaryObjects(unresolved = true)
    println("\n== Unused ==")
    printObjects(unused)
    println("\n== Used ==")
    printObjects(used)
    println("\n== Values ==")
    printValues(used, dictionary)
  }
}
