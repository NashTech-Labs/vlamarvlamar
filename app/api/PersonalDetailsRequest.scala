package api

import play.api.libs.json.{Format, Json}

case class PersonalDetailsRequest(
  firstName: String,
  lastName: String,
  gender: String,
  dateOfBirth: Long,
)

object PersonalDetailsRequest {
  implicit val format: Format[PersonalDetailsRequest] = Json.format
}
