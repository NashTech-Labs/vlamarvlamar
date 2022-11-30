package controllers

import api.{ErrorMessage, ErrorResponse}
import controllers.ResultCorsExtensions.CorsResult
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{BaseController, ControllerComponents, Result}

import scala.concurrent.ExecutionContext

trait ControllerHelper {
  this: BaseController =>
  val controllerComponents: ControllerComponents
  val configuration: Configuration
  implicit val ec: ExecutionContext = controllerComponents.executionContext

  def error(message: String, errors: List[ErrorMessage]): Result = {
    BadRequest(Json.toJson(ErrorResponse(400, message, errors))).withCors
  }

  def errorMessage(`type`: String, message: String): ErrorMessage = {
    ErrorMessage(`type` = `type`, message = message)
  }

  def unprocessableError(message: String, errors: List[ErrorMessage]): Result = {
    UnprocessableEntity(Json.toJson(ErrorResponse(422, message, errors))).withCors
  }
}
