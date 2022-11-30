package services

import models.Address
import persistence.{DynamoRecord, RegistrationTable}
import play.api.Configuration

import scala.concurrent.ExecutionContext

class Addresses(
                 val configuration: Configuration,
                 val registrationTable: RegistrationTable,
                 implicit val ec: ExecutionContext
               ) extends Service {

  def insertAddress(address: Address): Either[String, DynamoRecord] = {
    registrationTable.insertRecord(address.record)
  }
}
