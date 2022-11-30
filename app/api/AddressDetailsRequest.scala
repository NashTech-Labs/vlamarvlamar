package api

import play.api.libs.json.{Format, Json}

case class AddressDetailsRequest(
  street: String,
  street2: String,
  city: String,
  state: String,
  postalCode: String,
)

object AddressDetailsRequest {
  implicit val format: Format[AddressDetailsRequest] = Json.format
}