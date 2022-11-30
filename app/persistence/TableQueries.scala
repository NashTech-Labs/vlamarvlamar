package persistence

import models.WaitlistUsersTable
import slick.lifted.TableQuery
import persistence.PloomPostgresProfile.api._

object TableQueries {
  val waitlistUsersQ = TableQuery[WaitlistUsersTable]

  def print(): Unit = {
    println(schema)
  }

  val schema = (
    waitlistUsersQ.schema
  ).createStatements.reduce(_ + ";\n" + _)
}
