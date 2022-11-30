package api

import play.api.libs.json.{Format, Json}

case class ApplyReferralCode(
                              referralCode: String
                            )

object ApplyReferralCode {
  implicit val format: Format[ApplyReferralCode] = Json.format
}
