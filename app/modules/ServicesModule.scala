package modules
import play.api.BuiltInComponentsFromContext
import services._
import persistence._
import play.api.db.slick.{DbName, SlickComponents}
import play.api.libs.ws.ahc.AhcWSComponents

trait RegistrationDB
trait DefaultDB
trait ServicesModule {
  this: BuiltInComponentsFromContext with AhcWSComponents with SlickComponents with MainModule =>
  import com.softwaremill.macwire._

  lazy val dbConfig = slickApi.dbConfig[PloomPostgresProfile](DbName("default"))

  lazy val billingTable: BillingTable           = wire[BillingTable]
  lazy val geographyTable: GeographyTable       = wire[GeographyTable]
  lazy val registrationTable: RegistrationTable = wire[RegistrationTable]
  lazy val waitlistTable: WaitlistTable         = wire[WaitlistTable]
  lazy val security: Security                   = wire[Security]
  lazy val addresses: Addresses                 = wire[Addresses]
  lazy val networks: Networks                   = wire[Networks]
  lazy val postalCodes: PostalCodes             = wire[PostalCodes]
  lazy val segment: Segment                     = wire[Segment]
  lazy val stripe: Stripe                       = wire[Stripe]
  lazy val stripeCustomers: StripeCustomers     = wire[StripeCustomers]
  lazy val stripeProducts: StripeProducts       = wire[StripeProducts]
  lazy val users: Users                         = wire[Users]
  lazy val waitlistUsers: WaitlistUsers         = wire[WaitlistUsers]
}
