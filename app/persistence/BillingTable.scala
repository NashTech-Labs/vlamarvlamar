package persistence

import awscala.dynamodbv2._
import scala.concurrent.ExecutionContext
import play.api.Configuration


case class BillingTable(configuration: Configuration, implicit val ec: ExecutionContext) extends DynamoTable {
  val name: String = "billing"
  val awsKeyId: String = configuration.get[String]("aws.access.key.id")
  val awsAccessKey: String = configuration.get[String]("aws.secret.access.key")

  override implicit val dynamoDB: DynamoDB = DynamoDB(awsKeyId, awsAccessKey)
  override val table = dynamoDB.table(name).get
}

case class GeographyTable(configuration: Configuration, implicit val ec: ExecutionContext) extends DynamoTable {
  val name: String = "geography"
  val awsKeyId: String = configuration.get[String]("aws.access.key.id")
  val awsAccessKey: String = configuration.get[String]("aws.secret.access.key")

  override implicit val dynamoDB: DynamoDB = DynamoDB(awsKeyId, awsAccessKey)
  override val table: Table = dynamoDB.table(name).get
}

case class RegistrationTable(configuration: Configuration, implicit val ec: ExecutionContext) extends DynamoTable {
  val name: String = "registration"
  val awsKeyId: String = configuration.get[String]("aws.access.key.id")
  val awsAccessKey: String = configuration.get[String]("aws.secret.access.key")

  override implicit val dynamoDB: DynamoDB = DynamoDB(awsKeyId, awsAccessKey)
  override val table: Table = dynamoDB.table(name).get
}

case class WaitlistTable(configuration: Configuration, implicit val ec: ExecutionContext) extends DynamoTable {
  val name: String = "waitlist"
  val awsKeyId: String = configuration.get[String]("aws.access.key.id")
  val awsAccessKey: String = configuration.get[String]("aws.secret.access.key")

  override implicit val dynamoDB: DynamoDB = DynamoDB(awsKeyId, awsAccessKey)
  override val table: Table = dynamoDB.table(name).get
}
