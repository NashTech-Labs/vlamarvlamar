package api

import play.api.libs.json.{Format, Json}

case class ErrorResponse(
                          statusCode: Int,
                          message: String,
                          errors: List[ErrorMessage]
                        )

object ErrorResponse {
  implicit val format: Format[ErrorResponse] = Json.format
}

case class ErrorMessage(
                         `type`: String,
                         message: String,
                       )

object ErrorMessage {
  implicit val format: Format[ErrorMessage] = Json.format
}
