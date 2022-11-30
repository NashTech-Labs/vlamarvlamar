package modules
import play.api.i18n.Langs
import play.api.mvc._
import play.api.mvc.BodyParsers
import controllers._
import play.api.{BuiltInComponentsFromContext, BuiltInComponents, Configuration}
import services._
import persistence._
import play.api.libs.ws.ahc._
import play.api.db._
import play.api.db.slick._
import com.typesafe.config._

trait MainModule {
  this: BuiltInComponentsFromContext with AhcWSComponents =>
  import com.softwaremill.macwire._
}
