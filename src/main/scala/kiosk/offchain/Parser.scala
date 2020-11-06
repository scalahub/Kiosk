package kiosk.offchain

import play.api.libs.json.{JsResult, JsString, JsSuccess, JsValue, Json, Reads, Writes}

class Parser(implicit val dictionary: Dictionary) {
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

  implicit val readsRegId = new Reads[RegId.Id] {
    override def reads(json: JsValue): JsResult[RegId.Id] = JsSuccess(RegId.fromString(json.as[String]))
  }
  implicit val writesRegId = new Writes[RegId.Id] {
    override def writes(o: RegId.Id): JsValue = JsString(RegId.toString(o))
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
  implicit val formatQuantifier = Json.format[Quantifier]
  implicit val formatRegister = Json.format[RegisterDefiner]
  implicit val formatToken = Json.format[TokenDefiner]
  implicit val formatInput = Json.format[InputDefiner]
  implicit val formatOutput = Json.format[OutputDefiner]
  implicit val formatConstant = Json.format[Constant]
  implicit val formatProtocol = Json.format[Protocol]
}
