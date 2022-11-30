package services

import models.WaitlistUser
import persistence.{DynamoRecord, WaitlistTable}
import play.api.Configuration
import play.api.libs.json.Json

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class WaitlistUsers(
  val configuration: Configuration,
  val waitlistTable: WaitlistTable,
  implicit val ec: ExecutionContext
) extends Service {

  def generateSignUpCode(encryptedSignUpCode: String): Future[Option[String]] = {
    for {
      maybeCode <- retrieveWaitlistUserBySignUpCode(encryptedSignUpCode)
      x = maybeCode match {
        case Some(_) => Some(encryptedSignUpCode)
        case None => None
      }
    } yield x
  }

  def generateReferralCode(encryptedSignUpCode: String): Future[Option[String]] = {
    for {
      maybeCode <- retrieveWaitlistUserBySignUpCode(encryptedSignUpCode)
      x = maybeCode match {
        case Some(_) => Some(encryptedSignUpCode)
        case None => None
      }
    } yield x
  }

  def insertWaitlistUser(waitlistUser: WaitlistUser): Future[Option[WaitlistUser]] = {
    val insertedWaitlistUser = waitlistTable.insertRecord(waitlistUser.record)
    insertedWaitlistUser match {
      case Left(_) => Future.successful(None)
      case Right(_) =>
        waitlistUser.referredBy match {
          case Some(referredBy) =>
            incrementReferralCount(referredBy)
          case None => Future.successful(None)
        }

        Future.successful(Some(waitlistUser))
    }
  }

  def retrieveWaitlistUser(id: UUID): Future[Option[WaitlistUser]] = {
    val maybeWaitlistUser = waitlistTable.queryKindSk("WaitlistUser", id.toString).flatMap(WaitlistUser.hydrator).headOption
    Future.successful(maybeWaitlistUser)
  }

  def retrieveWaitlistUserByEmail(encryptedEmail: String): Future[Option[WaitlistUser]] = {
    val maybeWaitlistUser = waitlistTable.queryByPk(s"WaitlistUser#$encryptedEmail").flatMap(WaitlistUser.hydrator).headOption
    Future.successful(maybeWaitlistUser)
  }


  def retrieveWaitlistUserByReferralCode(referralCode: String): Future[Option[WaitlistUser]] = {
    val maybeWaitlistUser = waitlistTable.queryKindExternalOwnerId("WaitlistUser", referralCode.toUpperCase).flatMap(WaitlistUser.hydrator).headOption
    Future.successful(maybeWaitlistUser)
  }

  def retrieveWaitlistUserIdByReferralCode(referralCode: Option[String]): Future[Option[UUID]] = {
    val maybeId: Option[UUID] = referralCode match {
      case Some(code) =>
        if (code.equals("")) {
          None
        } else {
          val maybeWaitlistUser = waitlistTable.queryKindExternalOwnerId("WaitlistUser", code.toUpperCase).flatMap(WaitlistUser.hydrator).headOption
          maybeWaitlistUser match {
            case Some(waitlistUser) => Some(waitlistUser.id)
            case None => None
          }
        }
      case None => None
    }

    Future.successful(maybeId)
  }

  def retrieveWaitlistUserBySignUpCode(signUpCode: String): Future[Option[WaitlistUser]] = {
    val maybeWaitlistUser = waitlistTable.queryKindExternalId("WaitlistUser", signUpCode.toUpperCase).flatMap(WaitlistUser.hydrator).headOption
    Future.successful(maybeWaitlistUser)
  }

  def registerWaitlistUser(waitlistUser: WaitlistUser): Future[Option[WaitlistUser]] = {
    val testCode = "B7XEB8HF"
    val isTestCode: Boolean = waitlistUser.signUpCode == testCode
    if (isTestCode) {
      retrieveWaitlistUser(waitlistUser.id)
    } else {
      waitlistTable.insertRecord(waitlistUser.record) match {
        case Left(_) => Future.successful(None)
        case Right(value) =>
          val maybeWaitlistUser: Option[WaitlistUser] = WaitlistUser(Json.parse(value.json)).toOption
          Future.successful(maybeWaitlistUser)
      }
    }
  }

  def incrementReferralCount(waitlistUserId: UUID): Either[String, DynamoRecord] = {
    val maybeWaitlistUser = waitlistTable.queryKindSk("WaitlistUser", waitlistUserId.toString).flatMap(WaitlistUser.hydrator).headOption
    maybeWaitlistUser match {
      case Some(waitlistUser) =>
        val updatedWaitlistUser = waitlistUser.copy(referralCount = waitlistUser.referralCount + 1)
        waitlistTable.insertRecord(updatedWaitlistUser.record)
      case None => Left("Error Updating count")
    }
  }
}
