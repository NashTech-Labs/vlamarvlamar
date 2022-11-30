package api

import play.api.libs.json.{Format, Json}

case class CreateCustomerRequest(
  email: String,
)

object CreateCustomerRequest {
  implicit val format: Format[CreateCustomerRequest] = Json.format
}