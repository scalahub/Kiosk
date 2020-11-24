package kiosk.offchain.parser

import kiosk.ergo.{KioskBox, KioskType}
import kiosk.offchain.compiler.{CompileResult, model}
import kiosk.offchain.compiler.model.{InputOptions, _}
import play.api.libs.json._

object Parser {
  import scala.reflect.runtime.universe._

  def checkedReads[T](underlyingReads: Reads[T])(implicit typeTag: TypeTag[T]): Reads[T] = new Reads[T] {

    def classFields[T: TypeTag]: Set[String] =
      typeOf[T].members.collect {
        case m: MethodSymbol if m.isCaseAccessor => m.name.decodedName.toString
      }.toSet

    def reads(json: JsValue): JsResult[T] = {
      val caseClassFields = classFields[T]
      json match {
        case JsObject(fields) if (fields.keySet -- caseClassFields).nonEmpty =>
          JsError(s"Unexpected fields provided: ${(fields.keySet -- caseClassFields).mkString(", ")}")
        case _ => underlyingReads.reads(json)
      }
    }
  }

  private implicit val readsInputMatcherOptions = new Reads[InputOptions.Options] {
    override def reads(json: JsValue): JsResult[InputOptions.Options] = JsSuccess(InputOptions.fromString(json.as[String]))
  }
  private implicit val writesInputMatcherOptions = new Writes[InputOptions.Options] {
    override def writes(o: InputOptions.Options): JsValue = JsString(InputOptions.toString(o))
  }

  private implicit val readsUnaryConverter = new Reads[UnaryConverter.Converter] {
    override def reads(json: JsValue): JsResult[UnaryConverter.Converter] = JsSuccess(UnaryConverter.fromString(json.as[String]))
  }
  private implicit val writesUnaryConverter = new Writes[UnaryConverter.Converter] {
    override def writes(o: UnaryConverter.Converter): JsValue = JsString(UnaryConverter.toString(o))
  }

  private implicit val readsUnaryOperator = new Reads[UnaryOperator.Operator] {
    override def reads(json: JsValue): JsResult[UnaryOperator.Operator] = JsSuccess(UnaryOperator.fromString(json.as[String]))
  }
  private implicit val writesUnaryOperator = new Writes[UnaryOperator.Operator] {
    override def writes(o: UnaryOperator.Operator): JsValue = JsString(UnaryOperator.toString(o))
  }

  private implicit val readsBinaryOperator = new Reads[BinaryOperator.Operator] {
    override def reads(json: JsValue): JsResult[BinaryOperator.Operator] = JsSuccess(BinaryOperator.fromString(json.as[String]))
  }
  private implicit val writesBinaryOperator = new Writes[BinaryOperator.Operator] {
    override def writes(o: BinaryOperator.Operator): JsValue = JsString(BinaryOperator.toString(o))
  }

  private implicit val readsRegId = new Reads[RegNum.Num] {
    override def reads(json: JsValue): JsResult[RegNum.Num] = JsSuccess(RegNum.fromString(json.as[String]))
  }
  private implicit val writesRegId = new Writes[RegNum.Num] {
    override def writes(o: RegNum.Num): JsValue = JsString(RegNum.toString(o))
  }

  private implicit val readsQuantifierOp = new Reads[FilterOp.Op] {
    override def reads(json: JsValue): JsResult[FilterOp.Op] = JsSuccess(FilterOp.fromString(json.as[String]))
  }
  private implicit val writesQuantifierOp = new Writes[FilterOp.Op] {
    override def writes(o: FilterOp.Op): JsValue = JsString(FilterOp.toString(o))
  }

  private implicit val readsDataType = new Reads[DataType.Type] {
    override def reads(json: JsValue): JsResult[DataType.Type] = JsSuccess(DataType.fromString(json.as[String]))
  }
  private implicit val writesDataType = new Writes[DataType.Type] {
    override def writes(o: DataType.Type): JsValue = JsString(DataType.toString(o))
  }

  private implicit val readsBinaryOp = checkedReads(Json.reads[BinaryOp])
  private implicit val writesBinaryOp = Json.writes[BinaryOp]
  private implicit val readsUnaryOp = checkedReads(Json.reads[UnaryOp])
  private implicit val writesUnaryOp = Json.writes[UnaryOp]
  private implicit val readsConversion = checkedReads(Json.reads[Conversion])
  private implicit val writesConversion = Json.writes[Conversion]
  private implicit val readsLong = checkedReads(Json.reads[model.Long])
  private implicit val writesLong = Json.writes[model.Long]
  private implicit val readsRegister = checkedReads(Json.reads[Register])
  private implicit val writesRegister = Json.writes[Register]
  private implicit val readsAddress = checkedReads(Json.reads[Address])
  private implicit val writesAddress = Json.writes[Address]
  private implicit val readsId = checkedReads(Json.reads[Id])
  private implicit val writesId = Json.writes[Id]
  private implicit val readsToken = checkedReads(Json.reads[Token])
  private implicit val writesToken = Json.writes[Token]
  private implicit val readsInput = checkedReads(Json.reads[Input])
  private implicit val writesInput = Json.writes[Input]
  private implicit val readsOutput = checkedReads(Json.reads[Output])
  private implicit val writesOutput = Json.writes[Output]
  private implicit val readsConstant = checkedReads(Json.reads[model.Constant])
  private implicit val writesConstant = Json.writes[model.Constant]
  private implicit val readsProtocol = checkedReads(Json.reads[Protocol])
  private implicit val writesProtocol = Json.writes[Protocol]
  private implicit val writeKioskType = new Writes[KioskType[_]] {
    override def writes(o: KioskType[_]): JsValue = JsString(o.hex)
  }
  private implicit val writesKioskBox = Json.writes[KioskBox]
  implicit val writesCompileResult = Json.writes[CompileResult]

  def parse(string: String) = Json.parse(string).as[Protocol]
  def unparse(protocol: Protocol) = Json.toJson(protocol)
}
