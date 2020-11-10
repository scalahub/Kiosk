package kiosk.offchain.parser

import kiosk.offchain.model.{Address, BinaryOperator, CollByte, DataType, Input, Long, Output, Protocol, QuantifierOp, RangeFilter, RegNum, Register, Token, UnaryConverter, UnaryOperator}
import kiosk.offchain.{BinaryOp, Constant, Conversion, UnaryOp}
import play.api.libs.json._

object Parser {

  private implicit val readsUnaryConverter = new Reads[UnaryConverter.Converter] {
    override def reads(json: JsValue): JsResult[UnaryConverter.Converter] = JsSuccess(UnaryConverter.fromString(json.as[String]))
  }
  private implicit val writesUnaryConverter = new Writes[UnaryConverter.Converter] {
    override def writes(o: UnaryConverter.Converter): JsValue = JsString(UnaryConverter.toString(o))
  }

  private implicit val readsUnaryOp = new Reads[UnaryOperator.Operator] {
    override def reads(json: JsValue): JsResult[UnaryOperator.Operator] = JsSuccess(UnaryOperator.fromString(json.as[String]))
  }
  private implicit val writesUnaryOp = new Writes[UnaryOperator.Operator] {
    override def writes(o: UnaryOperator.Operator): JsValue = JsString(UnaryOperator.toString(o))
  }

  private implicit val readsBinaryOp = new Reads[BinaryOperator.Operator] {
    override def reads(json: JsValue): JsResult[BinaryOperator.Operator] = JsSuccess(BinaryOperator.fromString(json.as[String]))
  }
  private implicit val writesBinaryOp = new Writes[BinaryOperator.Operator] {
    override def writes(o: BinaryOperator.Operator): JsValue = JsString(BinaryOperator.toString(o))
  }

  private implicit val readsRegId = new Reads[RegNum.Num] {
    override def reads(json: JsValue): JsResult[RegNum.Num] = JsSuccess(RegNum.fromString(json.as[String]))
  }
  private implicit val writesRegId = new Writes[RegNum.Num] {
    override def writes(o: RegNum.Num): JsValue = JsString(RegNum.toString(o))
  }

  private implicit val readsQuantifierOp = new Reads[QuantifierOp.Op] {
    override def reads(json: JsValue): JsResult[QuantifierOp.Op] = JsSuccess(QuantifierOp.fromString(json.as[String]))
  }
  private implicit val writesQuantifierOp = new Writes[QuantifierOp.Op] {
    override def writes(o: QuantifierOp.Op): JsValue = JsString(QuantifierOp.toString(o))
  }

  private implicit val readsDataType = new Reads[DataType.Type] {
    override def reads(json: JsValue): JsResult[DataType.Type] = JsSuccess(DataType.fromString(json.as[String]))
  }
  private implicit val writesDataType = new Writes[DataType.Type] {
    override def writes(o: DataType.Type): JsValue = JsString(DataType.toString(o))
  }

  private implicit val formatBinaryOpResult = Json.format[BinaryOp]
  private implicit val formatUnaryOpResult = Json.format[UnaryOp]
  private implicit val formatUnaryConverterResult = Json.format[Conversion]
  private implicit val formatLongFilter = Json.format[RangeFilter]
  private implicit val formatLong = Json.format[Long]
  private implicit val formatRegister = Json.format[Register]
  private implicit val formatAddress = Json.format[Address]
  private implicit val formatCollByte = Json.format[CollByte]
  private implicit val formatToken = Json.format[Token]
  private implicit val formatInput = Json.format[Input]
  private implicit val formatOutput = Json.format[Output]
  private implicit val formatConstant = Json.format[Constant]
  private implicit val formatProtocol = Json.format[Protocol]

  def parse(string: String) = Json.parse(string).as[Protocol]
  def unparse(protocol: Protocol) = Json.toJson(protocol)
}
