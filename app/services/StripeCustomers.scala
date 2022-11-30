package services

import models.StripeCustomer
import persistence.BillingTable
import play.api.Configuration

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class StripeCustomers(
                       val configuration: Configuration,
                       val billingTable: BillingTable,
                       implicit val ec: ExecutionContext
                     ) extends Service {

  def getStripeCustomer(userId: UUID): Future[Option[StripeCustomer]] = {
    Future.successful(billingTable.queryByPk(s"StripeCustomer#$userId").flatMap(StripeCustomer.hydrator).headOption)
  }
}
