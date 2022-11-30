package api

import play.api.libs.json.{Format, Json}

case class RegisterBasicUserRequest(
  email: String,
  password: String,
  dateOfBirth: Long,
  gender: String,
  genderSelfIdentify: Option[String] = None,
  signUpCode: String,
)

object RegisterBasicUserRequest {
  implicit val format: Format[RegisterBasicUserRequest] = Json.format
}
