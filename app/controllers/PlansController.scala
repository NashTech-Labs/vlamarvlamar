package controllers

import controllers.ResultCorsExtensions.CorsResult
import models.{Price, StripePrice}
import persistence.BillingTable
import play.api.Configuration
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import services.{PostalCodes, Segment, StripeProducts, WaitlistUsers}

import scala.collection.mutable.ListBuffer

class PlansController(
  val configuration: Configuration,
  val controllerComponents: ControllerComponents,
  stripeProducts: StripeProducts,
  userAction: UserAction,
  table: BillingTable,
) extends BaseController with ControllerHelper {

  def getPlans(postalCode: String): Action[AnyContent] = userAction.async { implicit request =>
    val prices: ListBuffer[Price] = ListBuffer[Price]()

    val sProducts = for {
      sProducts <- stripeProducts.retrieveAllForPostalCode(postalCode)
    } yield sProducts

    val stripePrices = table.queryKind("StripePrice").flatMap(StripePrice.hydrator)

    val finalPrices = sProducts.map { results =>
      results.foreach { stripeProduct =>
        val filteredStripePrices = stripePrices
          .filter(_.stripeProductId.equals(stripeProduct.stripeId))
          .filter(_.stripeType.equals("one_time"))

        filteredStripePrices.foreach { stripePrice =>
          val price = Price(productId = stripeProduct.id, stripeProductId = stripeProduct.stripeId, priceId = stripePrice.id,
            stripePriceId = stripePrice.stripeId, name = stripeProduct.name, description = stripeProduct.description,
            price = stripePrice.unitAmount)

          prices += price
        }
      }

      prices.sortBy(_.price)
    }

    finalPrices.map { results =>
      val obj = JsObject(
        Map(
          "prices" -> Json.toJson(results)
        )
      )
      Ok(obj).withCors
    }
  }
}



