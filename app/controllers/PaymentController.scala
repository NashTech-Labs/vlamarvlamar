package controllers

import api.{ErrorMessage, PaymentIntentEphemeralResponse, PaymentIntentRequest, PaymentIntentResponse}
import com.stripe.model.EphemeralKey
import com.stripe.net.RequestOptions
import com.stripe.param.EphemeralKeyCreateParams
import controllers.ResultCorsExtensions.CorsResult
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, BaseController, ControllerComponents}
import services.Stripe

import scala.concurrent.Future

class PaymentController(
  val configuration: Configuration,
  val controllerComponents: ControllerComponents,
  userAction: UserAction,
  stripe: Stripe,
) extends BaseController with ControllerHelper {

  val publishableStripeKey: String = configuration.get[String]("stripe.api.key.publishable")

  def createPaymentIntent(): Action[JsValue] = userAction.async(parse.json) { implicit request =>
    request.body.validate[PaymentIntentRequest].fold(
      invalid = { _ =>
        val errorMessage = ErrorMessage("request", "Error registering details")
        Future.successful(unprocessableError("Error registering details", List(errorMessage)))
      },
      valid = { req =>
        val clientSecret = stripe.createPaymentIntent(req)
        val paymentIntentResponse = PaymentIntentResponse(clientSecret)

        Future.successful(Ok(Json.toJson(paymentIntentResponse)).withCors)
      })
  }

  def createPaymentIntentWithEphemeralKey(): Action[JsValue] = userAction.async(parse.json) { implicit request =>
    request.body.validate[PaymentIntentRequest].fold(
      invalid = { _ =>
        val errorMessage = ErrorMessage("request", "Error registering details")
        Future.successful(unprocessableError("Error registering details", List(errorMessage)))
      },
      valid = { req =>
        val clientSecret = stripe.createPaymentIntent(req)
        val ephemeralKeyParams = EphemeralKeyCreateParams.builder().setCustomer(req.customer).build()
        val options = RequestOptions.builder().setStripeVersionOverride("2022-08-01").build()
        val ephemeralKey = EphemeralKey.create(ephemeralKeyParams, options)
        val paymentIntentResponse = PaymentIntentEphemeralResponse(clientSecret, ephemeralKey.getSecret, publishableStripeKey)

        Future.successful(Ok(Json.toJson(paymentIntentResponse)).withCors)
      })
  }
}
