package kiosk.offchain.compiler

import kiosk.offchain.model._

object Compiler {
  def compile(protocol: Protocol) = {
    implicit val dictionary = new Dictionary
    declareProtocol(protocol).foreach {
      case (declaration, optIsMulti) =>
        println(s"Adding ${declaration.name.getOrElse("_")}: ${declaration.`type`} -> ${declaration.refs}")
        dictionary.addDeclaration(declaration, optIsMulti)
    }
    println("\n== Non-Lazy ==")
    dictionary.print(isLazy = false)
    println("\n== Lazy ==")
    dictionary.print(isLazy = true)
  }

  private def optSeq(s: Option[Seq[Declaration]])(implicit optIsMulti: Option[Boolean]) = s.toSeq.flatten.map((_, optIsMulti))

  private def opt(s: Option[Declaration])(implicit optIsMulti: Option[Boolean]) = s.toSeq.map((_, optIsMulti))

  private def optSeqToSeq[T](t: Option[Seq[T]]) = t.toSeq.flatten

  private def declareProtocol(p: Protocol) = {
    implicit val optIsMulti: Option[Boolean] = None
    optSeq(p.constants) ++ optSeq(p.unaryOps) ++ optSeq(p.binaryOps) ++ optSeq(p.conversions) ++
      optSeqToSeq(p.dataInputs).flatMap(declareInput) ++
      optSeqToSeq(p.inputs).flatMap(declareInput) ++
      optSeqToSeq(p.outputs).flatMap(declareOutput) ++
      p.fee.toSeq.flatMap(declareLong)
  }

  private def declareInput(input: Input) = {
    implicit val optIsMulti = Some(input.numBoxes.isDefined)

    opt(input.boxId) ++ opt(input.address) ++ optSeq(input.registers) ++
      optSeqToSeq(input.tokens).flatMap(declareToken) ++
      input.nanoErgs.toSeq.flatMap(declareLong) ++
      input.numBoxes.toSeq.flatMap(declareLong(_)(Some(false)))
  }

  private def declareOutput(output: Output) = {
    implicit val optIsMulti = Some(output.numBoxes.isDefined)

    Seq((output.address, optIsMulti)) ++ optSeq(output.registers) ++ optSeqToSeq(output.tokens).flatMap(declareToken) ++
      declareLong(output.nanoErgs) ++
      output.numBoxes.toSeq.flatMap(declareLong(_)(Some(false)))
  }

  private def declareToken(token: Token)(implicit optIsMulti: Option[Boolean]) = {
    opt(token.id) ++ token.numTokens.toSeq.flatMap(declareLong)
  }

  private def declareLong(long: Long)(implicit optIsMulti: Option[Boolean]) = {
    Seq((long, optIsMulti)) ++
      optSeq(long.filters)(Some(false))
  }
}
