package org.sh.kiosk.ergo

object ErgoMatcher {
  /*
    def isMatch(ergoScript:Text, varNames:Array[String], boxScript:Array[Byte]) = {
      val $ergoScript$:String = """
  {
    val x = blake2b256(c)
    b == 1234.toBigInt &&
    c == x &&
    d(0) == x
  }"""
      val $varNames$ = "[b,c,d]"
      val env = $convertedEnv
      varNames.map{
        name => (name, env.get(name).getOrElse(throw new Exception(s"No such key in env $name")))
      }.map{
        case (name, value) =>
          val serialized = value match {
            case grp: GroupElement => grp.getEncoded.toArray
            case bigInt: special.sigma.BigInt => bigInt.toBytes.toArray
            case collByte: Coll[Byte] => collByte.toArray
            case collCollByte: Coll[Coll[Byte]] => collCollByte.toArray.reduceLeft(_ ++ _):Array[Byte]
            case any => ???
          }

      }
      val script = compile(ergoScript)
      script

    }
  */

}
