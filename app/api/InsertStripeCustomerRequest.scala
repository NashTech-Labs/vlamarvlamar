package api

import play.api.libs.json.{Format, Json}

import java.util.UUID

case class InsertStripeCustomerRequest(
  userId: UUID,
  email: String,
)

object InsertStripeCustomerRequest {
  implicit val format: Format[InsertStripeCustomerRequest] = Json.format
}
