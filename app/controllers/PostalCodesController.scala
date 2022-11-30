package controllers

import api.ErrorMessage
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import services.PostalCodes

class PostalCodesController(
                             val configuration: Configuration,
                             val controllerComponents: ControllerComponents,
                             postalCodes: PostalCodes,
                           ) extends BaseController with ControllerHelper {

  def getStatusForPostalCode(postalCode: String): Action[AnyContent] = Action.async {
    postalCodes.getPostalCode(postalCode).map {
      case Some(pc) =>
        if (pc.canRegister)
          Ok(Json.obj("status" -> "enabled"))
        else
          Ok(Json.obj("status" -> "disabled"))
      case None =>
        val errorMessage = ErrorMessage("postalCode", "Invalid Postal Code")
        error("Invalid Postal Code", List(errorMessage))
    }
  }
}
