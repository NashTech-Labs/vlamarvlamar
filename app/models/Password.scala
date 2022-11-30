package models

import com.github.t3hnar.bcrypt._

final case class Password(value: String, salt: String) {
  private val peppered: String = s"$value$salt"
  val bcrypted: String = peppered.boundedBcrypt
  def valid(hash: String): Boolean = value.isBcryptedBounded(hash) || peppered.isBcryptedBounded(hash)
}
