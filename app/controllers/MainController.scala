package controllers

import play.api.mvc._


class MainController (val controllerComponents: ControllerComponents) extends BaseController {

  def index(): Action[AnyContent]  = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def options(path: String): Action[AnyContent] = Action {
    Ok("").withHeaders(
      "Access-Control-Allow-Origin" -> "*",
      "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "Accept, Origin, Content-type, X-Json, X-Prototype-Version, X-Requested-With, user_id, api_user, api_token, token, auth_token",
      "Access-Control-Allow-Credentials" -> "true",
      "Access-Control-Max-Age" -> (60 * 60 * 24).toString
    )
  }
}
