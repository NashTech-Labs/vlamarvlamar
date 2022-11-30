package api

import play.api.libs.json.{Format, Json}

case class CreateCustomerResponse(
  id: String, // Stripe ID
  email: String,
)

object CreateCustomerResponse {
  implicit val format: Format[CreateCustomerResponse] = Json.format
}
