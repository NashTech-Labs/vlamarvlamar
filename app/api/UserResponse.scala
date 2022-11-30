package api

import play.api.libs.json.{Format, Json}

import java.time.Instant
import java.util.UUID

case class UserResponse(
  id: UUID,
  email: String,
  firstName: Option[String] = None,
  lastName: Option[String] = None,
  dateOfBirth: Instant,
  gender: String,
  genderSelfIdentify: Option[String] = None,
  stripeCustomerId: Option[String],
  postalCode: Option[String] = None,
  waitlistUserId: Option[UUID] = None,
  authToken: Option[String] = None,
)

object UserResponse {
  implicit val format: Format[UserResponse] = Json.format
}

case class UserResponseV2(
                           id: UUID,
                           email: String,
                           stripeCustomerId: String,
                           waitlistUserId: UUID,
                           hasFriendReferralCode: Boolean,
                           authToken: String
                         )

object UserResponseV2 {
  implicit val format: Format[UserResponseV2] = Json.format
}

