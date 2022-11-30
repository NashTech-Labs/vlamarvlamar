package services

import models.Network
import persistence.GeographyTable
import play.api.Configuration

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class Networks (
                 val geographyTable: GeographyTable,
                 val configuration: Configuration,
                 implicit val ec: ExecutionContext
               ) extends Service {

  def getNetwork(id: UUID): Future[Option[Network]] = {
    val maybeNetwork = geographyTable.queryPkSk("Network", id.toString).flatMap(Network.hydrator).headOption
    Future.successful(maybeNetwork)
  }
}
