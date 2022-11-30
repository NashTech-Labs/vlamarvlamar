package api

import play.api.libs.json.{Format, Json}

case class LogInRequest(
  email: String,
  password: String,
)

object LogInRequest {
  implicit val format: Format[LogInRequest] = Json.format
}
