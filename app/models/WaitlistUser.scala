package models

import persistence.{DynamoItem, DynamoRecord}
import play.api.libs.json.{Format, JsSuccess, JsValue, Json}
import persistence.PloomPostgresProfile.api._

import java.time.Instant
import java.util.UUID

case class WaitlistUser(
                         id: UUID,
                         email: String,
                         status: String, // PENDING, INVITED, REDEEMED
                         signUpCode: String,
                         utmCampaign: Option[String] = None,
                         utmMedium: Option[String] = None,
                         utmSource: Option[String] = None,
                         userId: Option[UUID] = None,
                         postalCode: Option[String],
                         formerMember: Option[Boolean],
                         referralCode: String,
                         referredBy: Option[UUID] = None,
                         referralCount: Int = 0,
                         networkId: Option[UUID] = None,
                         createdAt: Instant,
                         updatedAt: Instant = Instant.now(),
                       ) extends DynamoItem {
  override def partitionKey: String = s"WaitlistUser#$email"

  override def sortKey: String = id.toString

  override def kind: String = "WaitlistUser"

  override def timestamp: Long = createdAt.toEpochMilli

  override def json: String = Json.stringify(Json.toJson(this))

  override def searchKey: String = s"${networkId.getOrElse("").toString}#$referralCount#${referredBy.getOrElse("").toString}"

  override def externalId: String = signUpCode

  override def externalOwnerId: String = referralCode

  override def ownerId: String = userId.getOrElse("none").toString
}

object WaitlistUser {
  implicit val format: Format[WaitlistUser] = Json.format

  val hydrator : DynamoRecord => Option[WaitlistUser] = r => WaitlistUser(Json.parse(r.json)).toOption

  def apply(json: JsValue): Either[String, WaitlistUser] = json.validate[WaitlistUser] match {
    case JsSuccess(o, _) => Right(o)
    case x               => Left(s"Failed to deserialize WaitlistUser $x")
  }
}

case class WaitlistUserPg(
                           id: UUID,
                           email: String,
                           status: String,
                           signUpCode: String,
                           utmCampaign: String = "",
                           utmMedium: String = "",
                           utmSource: String = "",
                           userId: Option[UUID] = None,
                           postalCode: Option[String],
                           formerMember: Option[Boolean],
                           createdAt: Long = Instant.now().toEpochMilli,
                           updatedAt: Long = Instant.now().toEpochMilli,
                         )

object WaitlistUserPg {
  implicit val format: Format[WaitlistUserPg] = Json.format
}

class WaitlistUsersTable(tag: Tag)
  extends Table[WaitlistUserPg](tag, "waitlist_users")
{
  def *   = (id, email, status, signUpCode, utmCampaign, utmMedium, utmSource, userId, postalCode, formerMember, createdAt, updatedAt) <> ((WaitlistUserPg.apply _).tupled, WaitlistUserPg.unapply)
  def id = column[UUID]("id", O.PrimaryKey)
  def email  = column[String]("email")
  def status  = column[String]("status")
  def signUpCode = column[String]("sign_up_code")
  def utmCampaign  = column[String]("utm_campaign")
  def utmMedium = column[String]("utm_medium")
  def utmSource = column[String]("utm_source")
  def userId = column[Option[UUID]]("user_id")
  def postalCode = column[Option[String]]("postal_code")
  def formerMember = column[Option[Boolean]]("former_member")
  def createdAt = column[Long]("created_at")
  def updatedAt = column[Long]("updated_at")
}

