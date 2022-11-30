package api

import play.api.libs.json.{Format, Json}

case class CreatePaymentResponse(
  clientSecret: String,
)

object CreatePaymentResponse {
  implicit val format: Format[CreatePaymentResponse] = Json.format
}
