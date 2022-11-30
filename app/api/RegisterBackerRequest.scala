package api

import play.api.libs.json.{Format, Json}

case class RegisterBackerRequest(
  email: String,
  password: String,
  firstName: String,
  lastName: String,
  dateOfBirth: Long,
  gender: String,
  signUpCode: String,
)

object RegisterBackerRequest {
  implicit val format: Format[RegisterBackerRequest] = Json.format
}
