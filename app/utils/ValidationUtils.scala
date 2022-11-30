package utils

import org.joda.time.{DateTime, Instant}
import scala.math.Ordering.Implicits._

object ValidationUtils {

  def validGender(gender: String): Boolean = {
    val validGenders = List("Female", "Male", "Non-Binary", "Self-Identify", "Prefer not to answer")

    validGenders.contains(gender)
  }

  def validAge(dob: Long): Boolean = {
    val instantDob = Instant.ofEpochMilli(dob)
    val minimumAge = DateTime.now().minusYears(18).toInstant

    instantDob <= minimumAge
  }

  def validEmail(email: String): Boolean = {
    """(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b""".r.unapplySeq(email).isDefined
  }
}
