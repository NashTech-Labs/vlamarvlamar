package utils

import java.util.UUID

object UUIDUtils {
  val zero: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
  def hash(value: String): UUID = UUID.nameUUIDFromBytes(value.getBytes())
}
