package kiosk.offchain

import kiosk.offchain.Parser.getFormatEither
import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue, Json, Reads, Writes}

import scala.util.Try

object Parser {
  def getFormatEither[A, B](implicit formatA: Format[A], formatB: Format[B]): Format[Either[A, B]] = {
    new Format[Either[A, B]] {
      override def writes(o: Either[A, B]): JsValue = o match {
        case Left(a)  => formatA.writes(a)
        case Right(b) => formatB.writes(b)
      }

      override def reads(json: JsValue): JsResult[Either[A, B]] = {
        Try {
          JsSuccess {
            Try(Left(json.as[A])).getOrElse(Right(json.as[B]))
          }
        }.recover {
          case ex => JsError(ex.getMessage)
        }
      }.getOrElse(JsError(s"Unknown error parsing $json"))
    }
  }
}
class Parser(implicit val dictionary: Dictionary) {

  implicit val readsUnaryConverter = new Reads[UnaryConverter.Converter] {
    override def reads(json: JsValue): JsResult[UnaryConverter.Converter] = JsSuccess(UnaryConverter.fromString(json.as[String]))
  }
  implicit val writesUnaryConverter = new Writes[UnaryConverter.Converter] {
    override def writes(o: UnaryConverter.Converter): JsValue = JsString(UnaryConverter.toString(o))
  }

  implicit val readsUnaryOp = new Reads[UnaryOp.Op] {
    override def reads(json: JsValue): JsResult[UnaryOp.Op] = JsSuccess(UnaryOp.fromString(json.as[String]))
  }
  implicit val writesUnaryOp = new Writes[UnaryOp.Op] {
    override def writes(o: UnaryOp.Op): JsValue = JsString(UnaryOp.toString(o))
  }

  implicit val readsBinaryOp = new Reads[BinaryOp.Op] {
    override def reads(json: JsValue): JsResult[BinaryOp.Op] = JsSuccess(BinaryOp.fromString(json.as[String]))
  }
  implicit val writesBinaryOp = new Writes[BinaryOp.Op] {
    override def writes(o: BinaryOp.Op): JsValue = JsString(BinaryOp.toString(o))
  }

  implicit val readsRegId = new Reads[RegNum.Num] {
    override def reads(json: JsValue): JsResult[RegNum.Num] = JsSuccess(RegNum.fromString(json.as[String]))
  }
  implicit val writesRegId = new Writes[RegNum.Num] {
    override def writes(o: RegNum.Num): JsValue = JsString(RegNum.toString(o))
  }

  implicit val readsQuantifierOp = new Reads[QuantifierOp.Op] {
    override def reads(json: JsValue): JsResult[QuantifierOp.Op] = JsSuccess(QuantifierOp.fromString(json.as[String]))
  }
  implicit val writesQuantifierOp = new Writes[QuantifierOp.Op] {
    override def writes(o: QuantifierOp.Op): JsValue = JsString(QuantifierOp.toString(o))
  }

  implicit val readsDataType = new Reads[DataType.Type] {
    override def reads(json: JsValue): JsResult[DataType.Type] = JsSuccess(DataType.fromString(json.as[String]))
  }
  implicit val writesDataType = new Writes[DataType.Type] {
    override def writes(o: DataType.Type): JsValue = JsString(DataType.toString(o))
  }

  implicit val formatBinaryOpResult = Json.format[BinaryOpResult]
  implicit val formatUnaryOpResult = Json.format[UnaryOpResult]
  implicit val formatUnaryConverterResult = Json.format[UnaryConverterResult]
  implicit val formatLongFilter = Json.format[Filter]
  implicit val formatLong = Json.format[Long]
  implicit val formatMultiLong = Json.format[MultiLong]
  implicit val formatSingleRegister = Json.format[Register]
  implicit val formatMultiRegister = Json.format[MultiRegister]
  implicit val formatAddress = Json.format[Address]
  implicit val formatCollByte = Json.format[CollByte]
  implicit val formatMultiCollByte = Json.format[MultiCollByte]
  implicit val formatSingleToken = Json.format[Token]
  implicit val formatMultiToken = Json.format[MultiToken]
  implicit val formatSingleInput = Json.format[SingleInput]
  implicit val formatMultiInput = Json.format[MultiInput]
  implicit val formatSinleOrMultiInput = getFormatEither[SingleInput, MultiInput]
  implicit val formatOutput = Json.format[MultiOutput]
  implicit val formatConstant = Json.format[Constant]
  implicit val formatProtocol = Json.format[Protocol]

}
