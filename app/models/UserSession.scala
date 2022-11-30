package models

import play.api.libs.json.{Format, Json}

import java.time.Instant
import java.util.UUID

case class UserSession(id: String, token: String, lastLogin: Instant)

object UserSession {
  implicit val format: Format[UserSession] = Json.format

  def generateToken(id: String, ts: Instant, secret: String): String = UUID.nameUUIDFromBytes(s"$id-$secret-$ts".getBytes()).toString

  def validateToken(session: UserSession, secret: String): Boolean = {
    val gToken = generateToken(session.id, session.lastLogin, secret)
    gToken == session.token
  }
}