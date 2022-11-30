package models

import persistence.{DynamoItem, DynamoRecord}
import play.api.libs.json.{Format, JsSuccess, JsValue, Json}

import java.time.Instant
import java.util.UUID

case class PostalCode(
                       id: UUID,
                       country: String,
                       postalCode: String,
                       state: Option[String],
                       defaultCity: Option[String] = None,
                       tier: Int,
                       status: String = "INELIGIBLE", // ELIGIBLE, APPROVED, APPROVED_WAITLIST
                       networkId: UUID,
                       createdAt: Instant = Instant.now(),
                       updatedAt: Instant = Instant.now()
                     ) extends DynamoItem {
  override def partitionKey: String = s"PostalCode#$postalCode"

  override def sortKey: String = id.toString

  override def kind: String = "USZipCode"

  override def timestamp: Long = Instant.now().toEpochMilli

  override def json: String = Json.stringify(Json.toJson(this))

  override def searchKey: String = s"$tier#${state.get}#${defaultCity.get}"

  override def ownerId: String = networkId.toString

  def canRegister: Boolean = List("APPROVED", "APPROVED_WAITLIST").contains(status)
}

object PostalCode {
  implicit val format: Format[PostalCode] = Json.format

  val hydrator : DynamoRecord => Option[PostalCode] = r => PostalCode(Json.parse(r.json)).toOption

  def apply(json: JsValue): Either[String, PostalCode] = json.validate[PostalCode] match {
    case JsSuccess(o, _) => Right(o)
    case x               => Left(s"Failed to deserialize PostalCode $x")
  }
}
