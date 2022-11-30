package services

import models.PostalCode
import persistence.GeographyTable
import play.api.Configuration

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class PostalCodes(
  val geographyTable: GeographyTable,
  val configuration: Configuration,
  implicit val ec: ExecutionContext
) extends Service {

  def getPostalCode(postalCode: String): Future[Option[PostalCode]] = {
    val maybePostalCode = geographyTable.queryByPk(s"PostalCode#$postalCode").flatMap(PostalCode.hydrator).headOption
    Future.successful(maybePostalCode)
  }

  def retrieveTierForPostalCode(postalCode: String): Future[Int] = {
    for {
      x <- getPostalCode(postalCode)
      y = x match {
        case Some(value) => value.tier
        case None => 100
      }
    } yield y
  }

  def getNetworkId(postalCode: String): Future[Option[UUID]] = {
    getPostalCode(postalCode).map {
      case Some(pc) => Some(pc.networkId)
      case None => None
    }
  }
}
