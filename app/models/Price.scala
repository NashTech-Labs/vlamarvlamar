package models

import play.api.libs.json.{Format, Json}

import java.util.UUID

case class Price(
  productId: UUID,
  stripeProductId: String,
  priceId: UUID,
  stripePriceId: String,
  name: String,
  description: String,
  price: Int
)

object Price {
  implicit val format: Format[Price] = Json.format
}
