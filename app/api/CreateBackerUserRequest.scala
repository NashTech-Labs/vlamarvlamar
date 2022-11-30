package api

import play.api.libs.json.{Format, Json}

case class CreateBackerUserRequest(
  email: String,
  password: String,
  firstName: String,
  lastName: String,
  gender: String,
  dateOfBirth: Long,
  createdAt: Long,
  updatedAt: Long
)

object CreateBackerUserRequest {
  implicit val format: Format[CreateBackerUserRequest] = Json.format
}
