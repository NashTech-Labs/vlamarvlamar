package models

import persistence.{DynamoRecord, RetrieveCriteria}
import play.api.libs.json.{Format, JsSuccess, JsValue, Json}

import java.time.Instant
import java.util.UUID

case class StripeProduct(
                          id: UUID,
                          stripeId: String,
                          name: String,
                          status: String,
                          description: String,
                          tier: String = "STANDARD",
                          metaData: Map[String, String],
                          createdAt: Instant = Instant.now,
                          updatedAt: Instant = Instant.now) extends persistence.DynamoItem {

  override def sortKey: String = id.toString

  override def json: String = Json.stringify(Json.toJson(this))

  override val kind: String = "StripeProduct"

  override def timestamp: Long = Instant.now().toEpochMilli

  def hydrate: Option[StripeProduct] = StripeProduct(Json.parse(json)).toOption

  override def searchKey: String = s"$name#$stripeId#$description"

  override def externalId: String = stripeId
}

object StripeProduct {
  implicit val format: Format[StripeProduct] = Json.format

  val hydrator : DynamoRecord => Option[StripeProduct] = r => StripeProduct(Json.parse(r.json)).toOption
  def apply(record: DynamoRecord): Option[StripeProduct] = StripeProduct(Json.parse(record.json)).toOption

  def apply(json: JsValue): Either[String, StripeProduct] = json.validate[StripeProduct] match {
    case JsSuccess(o, _) => Right(o)
    case x               => {
      Left(s"Failed to deserialize StripeProduct $x")
    }
  }

  def retrieveByStripeIdCriteria(stripeId: String): RetrieveCriteria = RetrieveCriteria("StripeProduct", stripeId)
  def retrieveByNameCriteria(name: String): RetrieveCriteria = RetrieveCriteria("StripeProduct", name)

}
