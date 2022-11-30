package controllers

import api.{ApplyReferralCode, ErrorMessage}
import controllers.ResultCorsExtensions.CorsResult
import models.WaitlistUser
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import services.{Emails, Networks, PostalCodes, Segment, Users, WaitlistUsers}
import utils.ValidationUtils.validEmail

import java.time.Instant
import java.util.UUID
import scala.concurrent.Future

class WaitlistController(
  val configuration: Configuration,
  val controllerComponents: ControllerComponents,
  networks: Networks,
  postalCodes: PostalCodes,
  security: services.Security,
  segment: Segment,
  userAction: UserAction,
  users: Users,
  waitlistUsers: WaitlistUsers,
) extends BaseController with ControllerHelper {

  val countdown: Long = configuration.get[String]("countdown.open").toLong
  val countdownOpen: Long = configuration.get[String]("countdown.open").toLong
  val countdownClose: Long = configuration.get[String]("countdown.close").toLong

  def addWaitlistUser(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[api.CreateWaitlistUserRequest].fold(
      invalid = { _ =>
        Future.successful(
          unprocessableError("Unable to register..",
            List(errorMessage("email", "Unable to register.")))
        )
      },
      valid = { req =>
        if (!validEmail(req.email.toLowerCase())) {
          Future.successful(
            error("Please enter a valid email.",
              List(errorMessage("email", "Please enter a valid email.")))
          )
        } else {
          val environment = configuration.get[String]("environment")
          val encryptedEmail = security.encrypt(req.email.toLowerCase).get

          val signUpCode = scala.util.Random.alphanumeric.take(8).mkString.toUpperCase
          val referralCode = scala.util.Random.alphanumeric.take(6).mkString.toUpperCase

          val waitlistUserId = UUID.nameUUIDFromBytes(s"${req.email.toLowerCase}}".getBytes())
          val waitlistUserToInsert = WaitlistUser(id = waitlistUserId, email = encryptedEmail,
            status = "PENDING", signUpCode = signUpCode, utmCampaign = req.utmCampaign, utmMedium = req.utmMedium,
            utmSource = req.utmSource, postalCode = Some(req.postalCode), formerMember = Some(req.formerMember),
            referralCode = referralCode, createdAt = Instant.now())

          if (environment.equals("prod")) {
            Future.successful(Ok(Json.obj("message" -> "The waitlist is closed.")).withCors)
          } else {
            val r = for {
              referredBy <- waitlistUsers.retrieveWaitlistUserIdByReferralCode(req.referralCode)
              uniqueSignUpCode <- waitlistUsers.retrieveWaitlistUserBySignUpCode(signUpCode)
              uniqueReferralCode <- waitlistUsers.retrieveWaitlistUserByReferralCode(referralCode)
              maybeNetworkId <- postalCodes.getNetworkId(req.postalCode)
              maybeNetwork <- maybeNetworkId match {
                case Some(networkId) => networks.getNetwork(networkId)
                case None => Future.successful(None)
              }
              alreadyRegistered <- waitlistUsers.retrieveWaitlistUserByEmail(encryptedEmail)
              insertWaitlistUser <- {
                uniqueSignUpCode match {
                  case Some(_) => Future.successful(None)
                  case None => uniqueReferralCode match {
                    case Some(_) => Future.successful(None)
                    case None => alreadyRegistered match {
                      case Some(existingWaitlistUser) =>
                        val updatedWaitlistUser = existingWaitlistUser.copy(postalCode = Some(req.postalCode),
                          formerMember = Some(req.formerMember), networkId = maybeNetworkId, updatedAt = Instant.now())
                        waitlistUsers.insertWaitlistUser(updatedWaitlistUser)
                        Emails.deliver(security.decrypt(updatedWaitlistUser.email).get, "waitlist-registration")
                        Future.successful(Some(updatedWaitlistUser))
                      case None => waitlistUsers.insertWaitlistUser(waitlistUserToInsert.copy(referredBy = referredBy,
                        networkId = maybeNetworkId))
                    }
                  }
                }
              }
              x = insertWaitlistUser match {
                case Some(waitlistUser) =>
                  val segmentProperties: Map[String, Any] = Map(
                    "email" -> req.email,
                    "networkId" -> maybeNetworkId.get,
                    "networkName" -> maybeNetwork.get.name,
                    "kind" -> "WaitlistUser",
                    "status" -> waitlistUser.status,
                    "signUpCode" -> waitlistUser.signUpCode,
                    "referralCode" -> waitlistUser.referralCode,
                    "referrals" -> waitlistUser.referralCount, // limitation on Mailchimp only
                    "waitlistUserId" -> waitlistUser.id,
                    "userId" -> waitlistUser.userId.getOrElse(""),
                    "formerMember" -> waitlistUser.formerMember.getOrElse(""),
                    "referredBy" -> waitlistUser.referredBy.getOrElse(""),
                    "postalCode" -> waitlistUser.postalCode.getOrElse(""),
                    "utmCampaign" -> waitlistUser.utmCampaign.getOrElse(""),
                    "utmMedium" -> waitlistUser.utmMedium.getOrElse(""),
                    "utmSource" -> waitlistUser.utmSource.getOrElse(""),
                    "createdAt" -> waitlistUser.createdAt,
                    "updatedAt" -> waitlistUser.updatedAt
                  )
                  segment.identify(waitlistUser.id, segmentProperties)
                  segment.track("Waitlist Registration", waitlistUser.id, Some(segmentProperties))
                  Emails.deliver(security.decrypt(waitlistUser.email).get, "waitlist-registration")
                  Ok(Json.obj(
                    "id" -> waitlistUser.id,
                    "email" -> security.decrypt(waitlistUser.email).get,
                    "status" -> waitlistUser.status,
                    "referralCode" -> waitlistUser.referralCode,
                    "referralCount" -> waitlistUser.referralCount
                  )).withCors
                case None =>
                  val errorMessage = ErrorMessage("waitlist", "Error joining the Waitlist")
                  error("Error joining the waitlist", List(errorMessage)).withCors
              }
            } yield x

            r.recover {
              case e =>
                val errorMsg = e.getMessage
                println(errorMsg)

                val key = errorMsg match {
                  case _ if errorMsg.contains("duplicate key value violates unique constraint \"waitlist_users_email_key\"") => "You have signed up for the waitlist!"
                  case _ => "Error joining the waitlist. Please try again."
                }

                val errorMessage = ErrorMessage("waitlist", key)
                error(key, List(errorMessage)).withCors
            }
          }
        }
      }
    )
  }

  def getWaitlistUser(email: String): Action[AnyContent] = Action.async {
    val encEmail = security.encrypt(email).get

    waitlistUsers.retrieveWaitlistUserByEmail(encEmail).map {
      case Some(waitlistUser: WaitlistUser) =>
        val result = Json.obj(
          "status" -> waitlistUser.status,
          "signUpCode" -> waitlistUser.signUpCode,
          "referralCode" -> waitlistUser.referralCode,
          "referralCount" -> waitlistUser.referralCount
        )
        Ok(Json.toJson(result)).withCors
      case None =>
        val result = Json.obj(
          "status" -> "WAITLIST",
          "signUpCode" -> "",
          "referralCode" -> "",
          "referralCount" -> 0
        )
        Ok(Json.toJson(result)).withCors
    }
  }

  def applyUserReferral(): Action[JsValue] = userAction.async(parse.json) { implicit request =>
    request.body.validate[ApplyReferralCode].fold(
      invalid = { _ =>
        Future.successful(
          unprocessableError("Unable to Apply Referral Code",
            List(errorMessage("email", "Unable to Apply Referral Code")))
        )
      },
      valid = { req =>
        val r = for {
          maybeUser <- users.retrieveUser(request.userId)
          maybeReferree <- maybeUser match {
            case Some(user) => waitlistUsers.retrieveWaitlistUserByEmail(user.email)
            case None => Future.successful(None)
          }
          maybeReferrer <- waitlistUsers.retrieveWaitlistUserByReferralCode(req.referralCode)
          updatedWaitlistUser <- maybeReferrer match {
            case Some(referrer) =>
                maybeReferree match {
                  case Some(referee) =>
                    if (referee.referredBy.isEmpty) {
                      val updatedWaitlistUser = referee.copy(referredBy = Some(referrer.id), updatedAt = Instant.now())
                      waitlistUsers.insertWaitlistUser(updatedWaitlistUser)

                      val segmentPropertiesReferee: Map[String, Any] = Map(
                        "email" -> security.decrypt(referee.email).get,
                        "referredBy" -> referrer.id
                      )

                      segment.identify(referee.id, segmentPropertiesReferee)

                      val segmentPropertiesReferer: Map[String, Any] = Map(
                        "email" -> security.decrypt(referrer.email).get,
                        "referrals" -> (referrer.referralCount + 1),
                      )

                      if (referrer.userId.isDefined) {
                        segment.identify(referrer.userId.get, segmentPropertiesReferer)
                      } else {
                        segment.identify(referrer.id, segmentPropertiesReferer)
                      }

                      Future.successful(Ok(Json.toJson(ApplyReferralCode(req.referralCode))))

                    } else {
                      val errorMessage = ErrorMessage("waitlist", "Already Referred")
                      Future.successful(error("Already Referred", List(errorMessage)))
                    }
                  case None =>
                    val errorMessage = ErrorMessage("waitlist", "Error 215: Applying Referral Code")
                    Future.successful(error("Error Applying Referral Code", List(errorMessage)))
                }
            case None =>
              val errorMessage = ErrorMessage("waitlist", "Error 219: Applying Referral Code")
              Future.successful(error("Error Applying Referral Code", List(errorMessage)))
          }
        } yield updatedWaitlistUser

        r.recover {
          case e =>
            val errorMsg = e.getMessage
            println(errorMsg)

            val key = errorMsg match {
              case _ => "Error Applying Referral Code. Please try again."
            }

            val errorMessage = ErrorMessage("waitlist", key)
            error(key, List(errorMessage)).withCors
        }
      }
    )
  }

  def getCountdown: Action[AnyContent] = Action {
    if (countdown < Instant.now().toEpochMilli) {
      Ok(s"""{"countdown": null}""").withCors
    } else {
      Ok(Json.obj("countdown" -> countdown)).withCors
    }
  }

  def getCountdowns: Action[AnyContent] = Action {
    Ok(Json.obj(
      "countdownOpen" -> countdownOpen,
      "countdownClose" -> countdownClose
    )).withCors
  }
}
