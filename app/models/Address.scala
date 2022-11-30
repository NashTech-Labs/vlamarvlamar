package models

import persistence.DynamoItem

import java.time.Instant
import play.api.libs.json.{Format, Json}

import java.util.UUID

case class Address(
  id: UUID,
  parentId: UUID,
  street: String,
  street2: Option[String],
  city: String,
  state: String,
  postalCode: String,
  kind: String, // ShippingAddress, BillingAddress, TheaterAddress
  status: String,
  networkId: UUID,
  createdAt: Instant = Instant.now(),
  updatedAt: Instant = Instant.now(),
) extends DynamoItem {

  override def partitionKey: String = s"Address#$parentId"

  override def sortKey: String = id.toString

  override def searchKey: String = s"$networkId#$state#$city"

  override def json: String = Json.stringify(Json.toJson(this))

  override def timestamp: Long = Instant.now().toEpochMilli

  override def ownerId: String = kind match {
    case "ShippingAddress" => s"User#${parentId.toString}"
    case "BillingAddress"  => s"User#${parentId.toString}"
    case "TheaterAddress"  => s"Theater#${parentId.toString}"
  }
}

object Address {
  implicit val format: Format[Address] = Json.format
}

case class CreateAddress(
  parentId: UUID,
  street: String,
  street2: Option[String],
  city: String,
  state: String,
  postalCode: String,
  kind: String,
)

object CreateAddress {
  implicit val format: Format[CreateAddress] = Json.format
}
