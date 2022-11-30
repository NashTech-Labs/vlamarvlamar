package controllers

import scala.concurrent._
import play.api.mvc._
import play.api.Configuration
import play.api.libs.json._
import java.util.UUID
import java.time.Clock
import pdi.jwt._

class UserAction(
  val configuration: Configuration,
  val parser: BodyParsers.Default
)
(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[AuthenticatedRequest, AnyContent] with Authentication{

  implicit val clock: Clock = Clock.systemUTC
  implicit val conf: Configuration = configuration
  val secretKey: String = configuration.get[String]("enc.secret.key")
  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {

    val result = for {
      //apiUser <- request.headers.get("api_user")
      //apiToken <- request.headers.get("api_token")
      userId <- request.headers.get("user_id")
      jwtToken <- request.headers.get("auth_token")
    } yield {
      val userSession = JwtSession.deserialize(jwtToken).claimData.validate[models.UserSession] match {
        case JsSuccess(value, path) => Some(value)
        case JsError(errors) => None
      }

      validRequest(userSession = userSession, apiUser = "apiUser", requestApiToken = "apiToken", userId = userId, jwtToken = jwtToken, secret = secretKey) match {
        case true =>
          block(new AuthenticatedRequest(UUID.fromString(userId), request))
        case false =>
          Future.successful{
            Results.Forbidden(JsObject(Map("errors" -> JsArray(Seq(JsString("Invalid credentials"))))))
          }
      }
    }

    result.fold{
      Future.successful{
        Results.BadRequest(JsObject(Map("errors" -> JsArray(Seq(JsString("Malformed Request Headers. api_user, api_token, user_id and token required"))))))
      }
    }{r => r}
  }

}