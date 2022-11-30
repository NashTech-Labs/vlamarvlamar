package models

import persistence.{DynamoItem, DynamoRecord}
import play.api.libs.json.{Format, JsSuccess, JsValue, Json}

import java.time.Instant
import java.util.UUID

case class Network(
                    id: UUID,
                    name: String,
                    status: String = "INELIGIBLE", // ELIGIBLE, APPROVED, APPROVED_WAITLIST
                    createdAt: Instant = Instant.now(),
                    updatedAt: Instant = Instant.now()
                  ) extends DynamoItem {

  override def partitionKey: String = "Network"

  override def sortKey: String = id.toString

  override def kind: String = "Network"

  override def timestamp: Long = Instant.now().toEpochMilli

  override def json: String = Json.stringify(Json.toJson(this))

  override def searchKey: String = name

  def isValidStatus(statusToCheck: String): Boolean = Seq("INELIGIBLE", "ELIGIBLE", "APPROVED", "APPROVED_WAITLIST")
    .contains(statusToCheck)
}

object Network {
  implicit val format: Format[Network] = Json.format

  val hydrator : DynamoRecord => Option[Network] = r => Network(Json.parse(r.json)).toOption

  def apply(json: JsValue): Either[String, Network] = json.validate[Network] match {
    case JsSuccess(o, _) => Right(o)
    case x               => Left(s"Failed to deserialize Network $x")
  }
}
