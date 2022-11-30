package services

import api.{CreatePaymentResponse, InsertStripeCustomerRequest, PaymentIntentRequest}
import com.stripe.Stripe
import com.stripe.model.{Customer, PaymentIntent}
import com.stripe.param.{CustomerCreateParams, PaymentIntentCreateParams}
import play.api.Configuration
import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}

class Stripe(
  val configuration: Configuration,
  implicit val ec: ExecutionContext
) extends Service {

  Stripe.apiKey = configuration.get[String]("stripe.api.key.secret")

  def insertStripeCustomer(iscr: InsertStripeCustomerRequest): Future[Customer] = {
    val metadata: Map[String, String] = Map("userId" -> iscr.userId.toString)

    val createParams = CustomerCreateParams.builder()
      .setEmail(iscr.email)
      .setMetadata(metadata.asJava)
      .build()

    Future.successful(Customer.create(createParams))
  }

  def createPaymentIntent(pir: PaymentIntentRequest): String = {
    val currency = pir.currency.getOrElse("usd")

    val createParams = PaymentIntentCreateParams.builder()
      .setCurrency(currency)
      .setAmount(pir.amount)
      .setCustomer(pir.customer)
      .setSetupFutureUsage(PaymentIntentCreateParams.SetupFutureUsage.ON_SESSION)
      .putMetadata("signUpCharge", "true")
      .putMetadata("stripePriceId", pir.metadata.stripePriceId)
      .putMetadata("stripeProductId", pir.metadata.stripeProductId)
      .build()

    // Create a PaymentIntent with the order amount and currency
    val intent = PaymentIntent.create(createParams)

    CreatePaymentResponse(intent.getClientSecret).clientSecret
  }
}
