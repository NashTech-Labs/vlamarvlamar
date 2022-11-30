package api

import play.api.libs.json.{Format, Json}

case class PaymentIntentResponse(
                                clientSecret: String
                                )

object PaymentIntentResponse {
  implicit val format: Format[PaymentIntentResponse] = Json.format
}


case class PaymentIntentEphemeralResponse(
                                           clientSecret: String,
                                           ephemeralKey: String,
                                           publishableKey: String,
                                         )

object PaymentIntentEphemeralResponse {
  implicit val format: Format[PaymentIntentEphemeralResponse] = Json.format
}