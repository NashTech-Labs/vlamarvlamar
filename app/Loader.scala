import com.softwaremill.macwire._
import play.api.{ApplicationLoader, BuiltInComponentsFromContext, Mode}
import play.api.mvc._
import play.api.ApplicationLoader.Context
import play.filters.HttpFiltersComponents
import play.api.db.slick._
import play.api.db.evolutions.EvolutionsComponents
import play.api.db.DBComponents
import play.api.db.HikariCPComponents
import router.Routes

import scala.concurrent._
import controllers._
import modules._
import play.api.libs.ws.ahc._

class Registration(context: Context) extends BuiltInComponentsFromContext(context)
  with AssetsComponents
  with HttpFiltersComponents
  with SlickComponents
  with DBComponents
  with HikariCPComponents
  with EvolutionsComponents
  with AhcWSComponents
  with MainModule
  with ServicesModule
  with ControllersModule
 {

  override implicit lazy val executionContext: ExecutionContext = actorSystem.dispatcher
  
  override lazy val router = {
    val prefix = "/"
    wire[Routes]
  }

  lazy val messagesActionBuilder = new DefaultMessagesActionBuilderImpl(playBodyParsers.defaultBodyParser, messagesApi)
  lazy val messagesControllerComponents = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    defaultActionBuilder,
    playBodyParsers,
    messagesApi,
    langs,
    fileMimeTypes,
    executionContext)

  actorSystem.log.info("Start DB Evolutions")
  //applicationEvolutions
  //println(s"End DB Evolutions ${applicationEvolutions.start()}")
  
  def onStart() = {
    actorSystem.log.info("Initialize:BillingService Actor")
  }

  onStart()
}

class Loader extends ApplicationLoader {
  override def load(context: Context) = {
    println("Load Registration Service")
    new Registration(context).application
  }
}