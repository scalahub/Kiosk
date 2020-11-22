package kiosk.explorer

object ExplorerTest {
  def main(args: Array[String]): Unit = {
    println(Explorer.getBoxById("17da0c63a00008ce3659d074a9cf3f2f473045fabdc2b8918114807de9ca5831"))
    println(Explorer.getBoxById("9e289deab858c3f14c7056e568cfa1026c01242d151a94b165268b650e9ac966"))
  }
}
