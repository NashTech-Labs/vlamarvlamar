package models

import persistence.{DynamoRecord, RetrieveCriteria}
import play.api.libs.json.{Format, JsSuccess, JsValue, Json}

import java.time.Instant
import java.util.UUID

case class StripePrice(
                        id: UUID,
                        productId: UUID,
                        stripeId: String,
                        stripeProductId: String,
                        nickname: Option[String],
                        unitAmount: Int,
                        currency: String,
                        status: String,
                        active: Boolean,
                        metaData: Map[String, String],
                        recurring: Option[StripePriceRecurring] = None,
                        stripeType: String,
                        tier: Int = 0,
                        createdAt: Instant = Instant.now,
                        updatedAt: Instant = Instant.now
                      ) extends persistence.DynamoItem {

  override def partitionKey: String = s"StripePrice#$productId"

  override def sortKey: String = id.toString

  override def json: String = Json.stringify(Json.toJson(this))

  override val kind: String = "StripePrice"

  override def timestamp: Long = Instant.now().toEpochMilli

  def hydrate: Option[StripePrice] = StripePrice(Json.parse(json)).toOption

  override def searchKey: String = s"$stripeProductId#$stripeId#$nickname#$active"

  override def ownerId: String = productId.toString

  override def externalOwnerId: String = stripeProductId

  override def externalId: String = stripeId
}

object StripePrice {
  implicit val format: Format[StripePrice] = Json.format

  val hydrator : DynamoRecord => Option[StripePrice] = r => StripePrice(Json.parse(r.json)).toOption
  def apply(record: DynamoRecord): Option[StripePrice] = {
    StripePrice(Json.parse(record.json)).toOption
  }

  def apply(json: JsValue): Either[String, StripePrice] = json.validate[StripePrice] match {
    case JsSuccess(o, _) => Right(o)
    case x               =>
      Left(s"Failed to deserialize StripePrice $x")
  }

  def retrieveSubscriptionById(id: String): RetrieveCriteria = RetrieveCriteria("StripePrice", id)
  def retrieveSubscriptionStripeId(stripeId: String): RetrieveCriteria = RetrieveCriteria("StripePrice", stripeId)
}

case class StripePriceRecurring(
  aggregateUsage: Option[String] = None,
  interval: String,
  intervalCount: Long,
  usageType: String,
  trialPeriodDays: Long,
)

object StripePriceRecurring {
  implicit val format: Format[StripePriceRecurring] = Json.format
}