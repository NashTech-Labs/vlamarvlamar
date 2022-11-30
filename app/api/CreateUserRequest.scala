package api

import play.api.libs.json.{Format, Json}

case class CreateUserRequest(
  email: String,
  password: String,
  gender: String,
  dateOfBirth: Long,
  createdAt: Long,
  updatedAt: Long
)

object CreateUserRequest {
  implicit val format: Format[CreateUserRequest] = Json.format
}
