package api

import play.api.libs.json.{Format, Json}

case class RegisterDetailsRequest(
  firstName: String,
  lastName: String,
  street: String,
  street2: String,
  city: String,
  state: String,
  postalCode: String,
)

object RegisterDetailsRequest {
  implicit val format: Format[RegisterDetailsRequest] = Json.format
}
