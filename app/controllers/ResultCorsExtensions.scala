package controllers

import play.api.mvc.Result

object ResultCorsExtensions {
  implicit class CorsResult(result: Result) {
    def withCors: Result = result.withHeaders(
      "Access-Control-Allow-Origin" -> "*"
      , "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS" // OPTIONS for pre-flight
      , "Access-Control-Allow-Headers" -> "Accept, Content-Type, Origin, X-Json, X-Prototype-Version, X-Requested-With, api_token, token, user_id, api_user"
      , "Access-Control-Allow-Credentials" -> "true"
      , "Access-Control-Max-Age" -> (60 * 60 * 24).toString
    )
  }
}