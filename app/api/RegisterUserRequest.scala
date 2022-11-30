package api

import play.api.libs.json.{Format, Json}

case class RegisterUserRequest(
                              email: String,
                              password: String,
                              dateOfBirth: Long,
                              gender: String,
                              genderSelfIdentify: Option[String] = None,
                              firstName: String,
                              lastName: String,
                              street: String,
                              street2: String,
                              city: String,
                              state: String,
                              postalCode: String
                              )

object RegisterUserRequest {
  implicit val format: Format[RegisterUserRequest] = Json.format
}
