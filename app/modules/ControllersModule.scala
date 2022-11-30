package modules
import play.api.mvc.BodyParsers
import controllers._
import play.api.BuiltInComponentsFromContext

trait ControllersModule {
  this: BuiltInComponentsFromContext with MainModule with ServicesModule =>

  import com.softwaremill.macwire._

  lazy val bp: BodyParsers.Default                          = wire[BodyParsers.Default]
  lazy val userAction: UserAction                           = wire[controllers.UserAction]
  lazy val mainController: MainController                   = wire[controllers.MainController]
  lazy val paymentController: PaymentController             = wire[controllers.PaymentController]
  lazy val plansController: PlansController                 = wire[controllers.PlansController]
  lazy val postalCodesController: PostalCodesController     = wire[controllers.PostalCodesController]
  lazy val usersController: UsersController                 = wire[controllers.UsersController]
  lazy val waitlistUsersController: WaitlistController      = wire[controllers.WaitlistController]
}
