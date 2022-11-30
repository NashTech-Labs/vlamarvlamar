package services

import models.User
import persistence.{DynamoRecord, RegistrationTable}
import play.api.Configuration

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class Users(
             val configuration: Configuration,
             implicit val ec: ExecutionContext,
             val registrationTable: RegistrationTable,
             val security: Security
           ) extends Service {

  def insertUser(user: User): Future[Either[String, DynamoRecord]] = {
    Future.successful(registrationTable.insertRecord(user.record))
  }

  def retrieveUser(id: UUID): Future[Option[User]] = {
    Future.successful(getUser(id))
  }

  def retrieveUserWithEmail(email: String): Future[Option[User]] = {
    security.encrypt(email.toLowerCase()) match {
      case Failure(exception) => Future.failed(exception)
      case Success(encryptedEmail) =>
        Future.successful(registrationTable.queryKindSearchKey("User", encryptedEmail).flatMap(User.hydrator).headOption)
    }
  }

  def updateUser(updatedUser: User): Either[String, DynamoRecord] = {
    val maybeUser = getUser(updatedUser.id)

    maybeUser match {
      case Some(user) => registrationTable.insertRecord(updatedUser.record)
      case None => Left("No User Found")
    }
  }

  def getUser(id: UUID): Option[User] = {
    registrationTable.queryByPk(s"User#$id").flatMap(User.hydrator).headOption
  }
}
