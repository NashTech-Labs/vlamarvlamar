package controllers

import models.UserSession
import play.api.Configuration
import play.api.mvc.{Request, WrappedRequest}

import java.util.UUID

class AuthenticatedRequest[A](val userId: UUID, request: Request[A]) extends WrappedRequest[A](request)

trait Authentication {
  val configuration: Configuration
  val serverApiToken: String = configuration.get[String]("auth.api.token")
  val apiClients: Set[String] = Set("mobile", "admin")

  def validRequest(userSession: Option[models.UserSession], apiUser: String, requestApiToken: String, userId: String, jwtToken:String, secret: String): Boolean = {
    validateApiClient(apiUser, requestApiToken, serverApiToken) &&
      validateUserId(userSession, userId) &&
      //validateExpiration(userSession) &&
      validateToken(userSession, secret)
  }

  def validateToken(userSession: Option[UserSession], secret: String): Boolean = userSession.fold{
    false
  }{ u => UserSession.validateToken(session = u, secret) }

  def validateUserId(userSession: Option[UserSession], userId: String): Boolean = userSession.exists { us => us.id == userId }
  def validateApiClient(apiUser: String, requestApiToken: String, apiToken: String): Boolean = true //apiClients(apiUser) && requestApiToken == apiToken
  def validateExpiration(userSession: Option[models.UserSession]): Boolean = userSession.exists { us => us.lastLogin.isAfter(utils.Time.monthAgo) }
}