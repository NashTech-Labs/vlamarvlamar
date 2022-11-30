package api

import play.api.libs.json.{Format, Json}

case class UpdateUserRequest(
  email: Option[String],
  firstName: Option[String],
  lastName: Option[String],
  dateOfBirth: Option[Long],
  gender: Option[String],
)

object UpdateUserRequest {
  implicit val format: Format[UpdateUserRequest] = Json.format
}
