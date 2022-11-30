package utils

import com.softwaremill.macwire._
import play.api.{Application, BuiltInComponentsFromContext}
import modules._
import play.api.ApplicationLoader.Context
import play.filters.HttpFiltersComponents
import router.Routes
import controllers._
import play.api.db.{DBComponents, HikariCPComponents}
import play.api.db.evolutions.EvolutionsComponents
import play.api.db.slick.SlickComponents
import play.api.libs.ws.ahc._

class ConsoleApp(app: Application, context: Context) extends BuiltInComponentsFromContext(context)
  with AssetsComponents
  with HttpFiltersComponents
  with SlickComponents
  with DBComponents
  with HikariCPComponents
  with EvolutionsComponents
  with AhcWSComponents
  with MainModule
  with ServicesModule
  with ControllersModule {
  override def configuration: play.api.Configuration = app.configuration
  override def environment: play.api.Environment = app.environment
  override def applicationLifecycle: play.api.inject.ApplicationLifecycle = context.lifecycle

  override lazy val router = {
    val prefix = "/"
    wire[Routes]
  }

  def gc(k: String) = configuration.get[String](k)
}

object ConsoleApp {
  def apply(app: Application, context: Context) = new ConsoleApp(app, context)
}