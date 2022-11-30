package controllers

import api.{ErrorMessage, InsertStripeCustomerRequest, RegisterDetailsRequest, RegisterUserRequest, UserResponse, UserResponseV2}
import controllers.ResultCorsExtensions.CorsResult
import models.{Address, Password, PostalCode, User, WaitlistUser}

import java.time.{Clock, Instant => JavaInstant}
import java.time.Instant
import pdi.jwt.JwtSession
import play.api.Configuration
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{Action, BaseController, ControllerComponents, Result}
import services.{Addresses, Networks, PostalCodes, Segment, Stripe, StripeCustomers, Users, WaitlistUsers}
import utils.ValidationUtils.{validAge, validEmail, validGender}

import java.util.{Date, UUID}
import scala.concurrent.Future
import scala.jdk.CollectionConverters._


class UsersController(
  val configuration: Configuration,
  val controllerComponents: ControllerComponents,
  security: services.Security,
  addresses: Addresses,
  networks: Networks,
  postalCodes: PostalCodes,
  segment: Segment,
  stripe: Stripe,
  stripeCustomers: StripeCustomers,
  users: Users,
  waitlistUsers: WaitlistUsers,
  userAction: UserAction,
) extends BaseController with ControllerHelper {

  implicit val clock: Clock = Clock.systemUTC
  implicit val conf: Configuration = configuration
  val salt: String = configuration.get[String]("enc.salt")

  def registerBasic(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[api.RegisterBasicUserRequest].fold(
      invalid = { _ =>
        val errorMessage = ErrorMessage("request", "Bad Request")
        Future.successful(unprocessableError("Bad Request", List(errorMessage)))
      },
      valid = { req =>
        if (!validAge(req.dateOfBirth)) {
          Future.successful(
            error("You must be 18 years or older to register",
              List(errorMessage("dateOfBirth", "You must be 18 years or older to register")))
          )
        } else if (!validGender(req.gender)) {
          Future.successful(
            error("Please enter a formatted gender identity.",
              List(errorMessage("gender", "Please enter a formatted gender identity.")))
          )
        } else if (!validEmail(req.email.toLowerCase)) {
          Future.successful(
            error("Please enter a valid email.",
              List(errorMessage("email", "Please enter a valid email.")))
          )
        } else {

          val encryptedEmail = security.encrypt(req.email.toLowerCase).get
          val hashedPassword = Password(req.password, salt).bcrypted
          val userId = UUID.nameUUIDFromBytes(s"${req.email}${Instant.now()}".getBytes)
          val user = User(id = userId, password = hashedPassword, email = encryptedEmail,
            dateOfBirth = JavaInstant.ofEpochMilli(req.dateOfBirth), gender = req.gender,
            genderSelfIdentify = req.genderSelfIdentify)

          waitlistUsers.retrieveWaitlistUserBySignUpCode(req.signUpCode).flatMap {
            case None =>
              val errorMessage = ErrorMessage("waitlist", "Error creating Account")
              Future.successful(error("Error creating Account", List(errorMessage)).withCors)

            case Some(waitlistUser) =>
              if (waitlistUser.status != "INVITED") {
                val errorMessage = ErrorMessage("waitlist", "Unable to register right now.")
                Future.successful(error("Unable to register right now.", List(errorMessage)).withCors)
              } else {
                users.retrieveUserWithEmail(req.email.toLowerCase).flatMap {
                  case Some(_) =>
                    val errorMessage = ErrorMessage("user", "Error 68: Unable to create account")
                    Future.successful(error("Error 68: Unable to create account", List(errorMessage)).withCors)
                  case None =>
                    val updatedWaitlistUser = waitlistUser.copy(userId = Some(userId), status = "REDEEMED", updatedAt = Instant.now())
                    val stripeCustomer = for {
                      _ <- waitlistUsers.registerWaitlistUser(updatedWaitlistUser)
                      _ <- users.insertUser(user)
                      stripeCustomer <- stripe.insertStripeCustomer(InsertStripeCustomerRequest(userId, req.email))
                    } yield stripeCustomer

                    val finishedRequest = stripeCustomer.map { stripeUser =>
                      segment.registerUser(waitlistUser.id, user.id, req.dateOfBirth)
                      val gender = req.genderSelfIdentify match {
                        case Some(genderSelfIdentify) => genderSelfIdentify
                        case None => req.gender
                      }
                      val segmentProperties: Map[String, Any] = Map(
                        "email" -> req.email,
                        "birthday" -> Date.from(Instant.ofEpochMilli(req.dateOfBirth)),
                        "gender" -> gender,
                        "kind" -> "User",
                        "status" -> user.status,
                        "role" -> user.role,
                        "userId" -> user.id,
                        "createdAt" -> waitlistUser.createdAt,
                        "updatedAt" -> waitlistUser.updatedAt
                      )

                      segment.identify(user.id, segmentProperties)

                      val jwtToken = JwtSession(jsClaim = Json.toJson(user.session(secret = security.secretKey)).as[JsObject])
                      val userResponse = UserResponse(id = user.id, email = security.decrypt(user.email).get,
                        dateOfBirth = user.dateOfBirth, gender = user.gender, genderSelfIdentify = user.genderSelfIdentify,
                        stripeCustomerId = Some(stripeUser.getId), waitlistUserId = Some(waitlistUser.id),
                        authToken = Some(jwtToken.serialize))

                      Ok(Json.toJson(userResponse)).withCors
                    }

                    finishedRequest.recover {
                      case e =>
                        val errorMessage = ErrorMessage("generic", e.getMessage)
                        println(e.getMessage)

                        error("generuc", List(errorMessage)).withCors
                    }
                }
              }
          }
        }
      }
    )
  }

  def registerDetails(): Action[JsValue] = userAction.async(parse.json) { implicit request =>
    request.body.validate[RegisterDetailsRequest].fold(
      invalid = { _ =>
        val errorMessage = ErrorMessage("request", "Bad Request")
        Future.successful(unprocessableError("Bad Request", List(errorMessage)))
      },
      valid = { req =>
        val encFirstName = security.encrypt(req.firstName).toOption
        val encLastName = security.encrypt(req.lastName).toOption

        val encStreet = security.encrypt(req.street).get
        val encStreet2: Option[String] = security.encrypt(req.street2).toOption
        val address = Address(id = UUID.nameUUIDFromBytes(s"${request.userId}${java.time.Instant.now().toEpochMilli}".getBytes),
          parentId = request.userId, street = encStreet, street2 = encStreet2, city = req.city, state = req.state,
          postalCode = req.postalCode, kind = "ShippingAddress", status = "ACTIVE", networkId = utils.UUIDUtils.zero)

        val r = for {
          maybePostalCode <- postalCodes.getPostalCode(req.postalCode)
          _ = maybePostalCode match {
            case Some(postalCode) => if (postalCode.canRegister) {
              "exists"
            } else {
              val errorMessage = ErrorMessage("postalCode", "This is not a supported postal code")
              error("This is not a supported postal code", List(errorMessage)).withCors
            }
            case None =>
              val errorMessage = ErrorMessage("postalCode", "This is not a supported postal code")
              error("This is not a supported postal code", List(errorMessage)).withCors
          }
          maybeNetworkId <- postalCodes.getNetworkId(maybePostalCode.get.postalCode)
          maybeNetwork <- networks.getNetwork(maybeNetworkId.get)
          maybeUser <- users.retrieveUser(request.userId)
          _ = maybeUser match {
            case Some(user) =>
              users.updateUser(user.copy(firstName = encFirstName, lastName = encLastName, updatedAt = java.time.Instant.now()))
            case None =>
              val errorMessage = ErrorMessage("user", "TError 173: Updating User")
              error("Error 173: Updating User", List(errorMessage)).withCors
          }
          _ = addresses.insertAddress(address.copy(networkId = maybeNetworkId.get))
          stripeCustomer <- stripeCustomers.getStripeCustomer(request.userId)
          response = maybeUser match {
            case Some(user) =>
              val properties: Map[String, Any] = Map("firstName" -> req.firstName, "lastName" -> req.lastName,
                                                      "email" -> security.decrypt(user.email).get,
                                                      "networkId" -> maybeNetworkId.get, "networkName" -> maybeNetwork.get.name,
                                                      "postalCode" -> req.postalCode,
                                                      "address" -> Map("city" -> req.city, "state" -> req.state,
                                                                    "postalCode" -> req.postalCode).asJava,
                                                      "updatedAt" -> user.updatedAt)
              segment.identify(request.userId, properties)

              val userResponse = UserResponse(id = user.id, email = security.decrypt(user.email).get,
                dateOfBirth = user.dateOfBirth, gender = user.gender, stripeCustomerId = Some(stripeCustomer.get.stripeId),
                postalCode = Some(req.postalCode))

              Ok(Json.toJson(userResponse)).withCors
            case None =>
              val errorMessage = ErrorMessage("user", "Error 195: Updating User")
              error("Error 195: Updating User", List(errorMessage)).withCors
          }
        } yield response

        r.recover {
          case e =>
            println(s"e: $e")
            val errorMsg = e.getMessage
            println(errorMsg)

            val key = errorMsg match {
              case _ if errorMsg.contains("duplicate key value violates unique constraint \"users_email_key\"") => "You have signed up!"
              case _ => "Error continuing registration. Please try again."
            }

            val errorMessage = ErrorMessage("user", key)
            error("Error 195: Updating User", List(errorMessage)).withCors
        }
      }
    )
  }

  def register(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[api.RegisterUserRequest].fold(
      invalid = { _ =>
        val errorMessage = ErrorMessage("request", "Bad Request")
        Future.successful(unprocessableError("Bad Request", List(errorMessage)))
      },
      valid = { req =>
        if (!validAge(req.dateOfBirth)) {
          Future.successful(
            error("You must be 18 years or older to register",
            List(errorMessage("dateOfBirth", "You must be 18 years or older to register")))
          )
        } else if (!validGender(req.gender)) {
          Future.successful(
            error("Please enter a formatted gender identity.",
            List(errorMessage("gender", "Please enter a formatted gender identity.")))
          )
        } else if (!validEmail(req.email.toLowerCase)) {
          Future.successful(
            error("Please enter a valid email.",
              List(errorMessage("email", "Please enter a valid email.")))
          )
        } else {

          val encryptedEmail = security.encrypt(req.email.toLowerCase).get
          val hashedPassword = Password(req.password, salt).bcrypted
          val userId = UUID.nameUUIDFromBytes(s"${req.email}${Instant.now()}".getBytes)
          val encFirstName = security.encrypt(req.firstName).toOption
          val encLastName = security.encrypt(req.lastName).toOption
          val encStreet = security.encrypt(req.street).get
          val encStreet2: Option[String] = security.encrypt(req.street2).toOption
          val address = Address(id = UUID.nameUUIDFromBytes(s"${userId}${java.time.Instant.now().toEpochMilli}".getBytes),
            parentId = userId, street = encStreet, street2 = encStreet2, city = req.city, state = req.state,
            postalCode = req.postalCode, kind = "ShippingAddress", status = "ACTIVE", networkId = utils.UUIDUtils.zero)

          val user = User(id = userId, password = hashedPassword, email = encryptedEmail,
            dateOfBirth = JavaInstant.ofEpochMilli(req.dateOfBirth), gender = req.gender,
            genderSelfIdentify = req.genderSelfIdentify, firstName = encFirstName, lastName = encLastName)

          postalCodes.getPostalCode(req.postalCode) flatMap {
            case None =>
              Future.successful(error("Unable to register right now",
                List(errorMessage("postalCode", "Unable to register right now"))))
            case Some(postalCode) =>
              waitlistUsers.retrieveWaitlistUserByEmail(encryptedEmail).flatMap {
                case None =>
                  if (postalCode.status.equals("APPROVED")) {
                    val signUpCode = scala.util.Random.alphanumeric.take(8).mkString.toUpperCase
                    val referralCode = scala.util.Random.alphanumeric.take(6).mkString.toUpperCase

                    val waitlistUserId = UUID.nameUUIDFromBytes(s"${req.email.toLowerCase}}".getBytes())
                    val waitlistUserToInsert = WaitlistUser(id = waitlistUserId, email = encryptedEmail,
                      status = "PENDING", signUpCode = signUpCode, postalCode = Some(req.postalCode), formerMember = Some(false),
                      referralCode = referralCode, createdAt = Instant.now())
                    waitlistUsers.insertWaitlistUser(waitlistUserToInsert)

                    registration(req, waitlistUserToInsert, address, user, postalCode)
                  } else {
                    Future.successful(error("Error creating Account.",
                      List(errorMessage("waitlist", "Error creating Account"))))
                  }

                case Some(waitlistUser) => registration(req, waitlistUser, address, user, postalCode)
              }
          }
        }
      }
    )
  }

  def registration(req: RegisterUserRequest, waitlistUser: WaitlistUser, address: Address, user: User, postalCode: PostalCode): Future[Result] = {
        if (waitlistUser.status.equals("INVITED") || postalCode.status.equals("APPROVED"))
          users.retrieveUserWithEmail(req.email.toLowerCase).flatMap {
            case Some(_) =>
              Future.successful(error("Error 231: Unable to create account",
                List(errorMessage("user", "Error 231: Unable to create account"))))
            case None =>

              val updatedWaitlistUser = waitlistUser.copy(userId = Some(user.id), status = "REDEEMED", updatedAt = Instant.now())
              val r = for {
                maybePostalCode <- postalCodes.getPostalCode(req.postalCode)
                maybeNetworkId <- postalCodes.getNetworkId(maybePostalCode.get.postalCode)
                maybeNetwork <- networks.getNetwork(maybeNetworkId.get)
                _ = addresses.insertAddress(address.copy(networkId = maybeNetworkId.get))
                _ <- waitlistUsers.registerWaitlistUser(updatedWaitlistUser)
                maybeUser <- users.insertUser(user)
                stripeCustomer <- stripe.insertStripeCustomer(InsertStripeCustomerRequest(user.id, req.email))

                response = maybeUser match {
                  case Right(_) =>
                    val gender = req.genderSelfIdentify match {
                      case Some(genderSelfIdentify) => genderSelfIdentify
                      case None => req.gender
                    }

                    val properties: Map[String, Any] = Map("firstName" -> req.firstName, "lastName" -> req.lastName,
                      "email" -> security.decrypt(user.email).get, "role" -> user.role, "userId" -> user.id,
                      "networkId" -> maybeNetworkId.get, "networkName" -> maybeNetwork.get.name,
                      "postalCode" -> req.postalCode, "birthday" -> Date.from(Instant.ofEpochMilli(req.dateOfBirth)),
                      "gender" -> gender, "kind" -> "User", "status" -> user.status,
                      "address" -> Map("city" -> req.city, "state" -> req.state,
                        "postalCode" -> req.postalCode).asJava, "createdAt" -> waitlistUser.createdAt,
                      "updatedAt" -> user.updatedAt)
                    segment.identify(user.id, properties)
                    segment.registerUser(waitlistUser.id, user.id, req.dateOfBirth)

                    val jwtToken = JwtSession(jsClaim = Json.toJson(user.session(secret = security.secretKey)).as[JsObject])

                    val userResponse = UserResponseV2(id = user.id, email = security.decrypt(user.email).get,
                      stripeCustomerId = stripeCustomer.getId, waitlistUserId = waitlistUser.id,
                      hasFriendReferralCode = waitlistUser.referredBy.isDefined, authToken = jwtToken.serialize)

                    Ok(Json.toJson(userResponse)).withCors
                  case Left(message) =>
                    val errorMessage = ErrorMessage("user", message)
                    error(message, List(errorMessage))
                }
              } yield response

              r.recover {
                case e =>
                  val errorMsg = e.getMessage
                  println(errorMsg)

                  errorMsg match {
                    case "This is not a supported postal code" => error(errorMsg, List(errorMessage("postalCode", errorMsg)))
                    case _ => error(errorMsg, List(errorMessage("general", errorMsg)))
                  }
              }
          }
        else
          Future.successful(error("Unable to register right now",
            List(errorMessage("waitlist", "Unable to register right now"))))
  }
}
