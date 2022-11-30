package services

import persistence.PloomPostgresProfile
import play.api.Configuration
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcBackend

import scala.concurrent.ExecutionContext

trait Service {
  val configuration: Configuration
  implicit val ec: ExecutionContext
}

trait DB {
  val dbConfig: DatabaseConfig[PloomPostgresProfile]
  val db: JdbcBackend#DatabaseDef = dbConfig.db
  //val registrationDb: JdbcBackend#DatabaseDef = registrationDbConfig.db
}
