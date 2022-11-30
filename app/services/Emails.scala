package services

import com.mashape.unirest.http.Unirest
import com.typesafe.config.ConfigFactory
import utils.ConfigKeys

import scala.util.Try

object Emails {
  private val config =  ConfigFactory.load()
  val apiKey: String = config.getString(ConfigKeys.mailgunApiKey)

  def deliver(recipient: String, template: String): Try[Unit] = Try {
    val subject = template match {
      case "waitlist-registration" => "Your spot to join MoviePass is saved!"
      case _ => ""
    }

    Unirest.post("https://api.mailgun.net/v3/mg.moviepass.com/messages")
      .basicAuth("api", apiKey)
      .queryString("from", "MoviePass <no-reply@moviepass.com>")
      .queryString("to", recipient)
      .queryString("subject", subject)
      .queryString("template", template)
      .asJson()
  }
}