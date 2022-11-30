package models

import play.api.libs.json.{Format, Json}

case class Plan(
  stripeProductId: String,
  stripePriceId: String,
  name: String,
  description: String,
  price: Long,
  interval: String,
)

object Plan {
  implicit val format: Format[Plan] = Json.format
}
