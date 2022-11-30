package api

import play.api.libs.json.{Format, Json}

case class CreateWaitlistUserRequest(
  email: String,
  postalCode: String,
  formerMember: Boolean,
  referralCode: Option[String] = None,
  utmCampaign: Option[String] = None,
  utmMedium: Option[String] = None,
  utmSource: Option[String] = None,
)

object CreateWaitlistUserRequest {
  implicit val format: Format[CreateWaitlistUserRequest] = Json.format
}
