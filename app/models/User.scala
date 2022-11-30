package models

import persistence.{DynamoItem, DynamoRecord}
import play.api.libs.json.{Format, JsSuccess, JsValue, Json}

import java.time.Instant
import java.util.UUID

case class User(
                 id: UUID,
                 email: String,
                 password: String,
                 firstName: Option[String] = None,
                 lastName: Option[String] = None,
                 dateOfBirth: Instant,
                 gender: String, // Female, Male, Non-Binary, Self-Identify, Prefer not to answer
                 genderSelfIdentify: Option[String] = None,
                 role: String = "STANDARD",
                 status: String = "PENDING", // PENDING, PENDING_ACTIVATION, ACTIVE, PAST_DUE, CANCELED, CANCELED_PAST_DUE
                 createdAt: Instant = Instant.now,
                 updatedAt: Instant = Instant.now,
               ) extends DynamoItem {

  override def partitionKey: String = s"User#${id.toString}"

  override def sortKey: String = id.toString

  override def kind: String = "User"

  override def searchKey: String = s"$email"

  override def json: String = Json.stringify(Json.toJson(this))

  override def timestamp: Long = Instant.now().toEpochMilli

  def session(secret: String): UserSession = {
    val ts = java.time.Instant.now
    val token = UUID.nameUUIDFromBytes(s"$id-$secret-$ts".getBytes()).toString
    UserSession(id = id.toString, token = token, lastLogin = ts)
  }
}

object User {
  implicit val format: Format[User] = Json.format

  val hydrator : DynamoRecord => Option[User] = r => User(Json.parse(r.json)).toOption
  def apply(record: DynamoRecord): Option[User] = {
    User(Json.parse(record.json)).toOption
  }

  def apply(json: JsValue): Either[String, User] = json.validate[User] match {
    case JsSuccess(o, _) => Right(o)
    case x               =>
      Left(s"Failed to deserialize User $x")
  }
}
