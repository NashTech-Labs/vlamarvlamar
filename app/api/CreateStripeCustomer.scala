package api

import play.api.libs.json.{Format, Json}

import java.util.UUID

case class CreateStripeCustomer(
  userId: UUID,
  stripeCustomerId: String,
)

object CreateStripeCustomer {
  implicit val format: Format[CreateStripeCustomer] = Json.format
}
