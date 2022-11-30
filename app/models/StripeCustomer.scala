package models

import persistence.DynamoRecord
import play.api.libs.json.{Format, JsSuccess, JsValue, Json}

import java.time.Instant
import java.util.UUID

case class StripeCustomer(
                           id: UUID,
                           userId: UUID,
                           stripeId: String,
                           address: Option[StripeAddress] = None,
                           description: Option[String] = None,
                           email: Option[String] = None,
                           name: Option[String] = None,
                           phone: Option[String] = None,
                           shipping: Option[StripeShipping] = None,
                           status: String,
                           metaData: Map[String, String],
                           createdAt: Instant = Instant.now,
                           updatedAt: Instant = Instant.now
                         ) extends persistence.DynamoItem {
  override def partitionKey: String = s"StripeCustomer#$userId"

  override def sortKey: String = id.toString

  override def json: String = Json.stringify(Json.toJson(this))

  override val kind: String = "StripeCustomer"

  override def timestamp: Long = Instant.now().toEpochMilli

  def hydrate: Option[StripeCustomer] = StripeCustomer(Json.parse(json)).toOption

  override def searchKey: String = s"$stripeId#$status"

  override def externalId: String = stripeId
}

object StripeCustomer {
  implicit val format: Format[StripeCustomer] = Json.format

  val hydrator: DynamoRecord => Option[StripeCustomer] = r => StripeCustomer(Json.parse(r.json)).toOption
  def apply(record: DynamoRecord): Option[StripeCustomer] = StripeCustomer(Json.parse(record.json)).toOption

  def apply(json: JsValue): Either[String, StripeCustomer] = json.validate[StripeCustomer] match {
    case JsSuccess(o, _) => Right(o)
    case x               => Left(s"Failed to deserialize StripeCustomer $x")
  }
}

case class StripeAddress(
                          line1: Option[String],
                          line2: Option[String],
                          city: Option[String],
                          state: Option[String],
                          postalCode: Option[String],
                          country: Option[String]
                        )

object StripeAddress {
  implicit val format: Format[StripeAddress] = Json.format
}

case class StripeShipping(
                           address: StripeAddress,
                           name: String,
                           phone: String
                         )

object StripeShipping {
  implicit val format: Format[StripeShipping] = Json.format
}