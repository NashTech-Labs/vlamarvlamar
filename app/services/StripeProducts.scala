package services

import models.StripeProduct
import persistence.{BillingTable, PloomPostgresProfile}
import play.api.Configuration
import slick.basic.DatabaseConfig
import scala.concurrent.{ExecutionContext, Future}

class StripeProducts(
  val dbConfig: DatabaseConfig[PloomPostgresProfile],
  val configuration: Configuration,
  table: BillingTable,
  postalCodes: PostalCodes,
  implicit val ec: ExecutionContext,
) extends Service with DB {

  def retrieveAllForPostalCode(postalCode: String): Future[List[StripeProduct]] = {
    val sps = table.queryKind("StripeProduct").flatMap(StripeProduct.hydrator)

    for {
      a <- postalCodes.retrieveTierForPostalCode(postalCode)
      prods <- {
        val plans: Seq[StripeProduct] = sps.filter(sp => sp.status == "Active")
                        .filter(sp => sp.metaData.isDefinedAt("Zone"))
                        .filter(sp => sp.metaData("Zone") == a.toString)
                        .filter(sp => sp.metaData.isDefinedAt("Kind"))
                        .filter(sp => sp.metaData("Kind") == "Subscription")
          .toList
        Future.successful(plans)
      }
    } yield prods
  }
}
