package api

import play.api.libs.json.{Format, Json}

case class PaymentIntentRequest(
 amount: Long,
 customer: String, // Stripe Customer ID
 currency: Option[String],
 metadata: MetaData,
)

object PaymentIntentRequest {
  implicit val format: Format[PaymentIntentRequest] = Json.format
}

case class MetaData(
  stripePriceId: String,
  stripeProductId: String,
)

object MetaData {
  implicit val format: Format[MetaData] = Json.format
}
