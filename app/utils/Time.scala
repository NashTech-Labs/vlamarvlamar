package utils

import org.joda.time.LocalDate

import java.sql.Timestamp
import java.time.{Instant, ZonedDateTime}
import java.time.temporal.ChronoUnit
import java.util.Date

object Time {
  val distantFuture: Instant = Instant.ofEpochSecond(4102444800L)
  def now: Timestamp            = new Timestamp(new Date().getTime)
  def thirtyMinutesFromNow: Instant = ZonedDateTime.now().plusMinutes(30).toInstant.truncatedTo(ChronoUnit.SECONDS)
  def monthFromNow: Instant = ZonedDateTime.now().plusMonths(1).toInstant().truncatedTo(ChronoUnit.SECONDS)
  def thirtyMinutesAgo: Instant = ZonedDateTime.now().minusMinutes(30).toInstant.truncatedTo(ChronoUnit.SECONDS)
  def monthAgo: Instant = ZonedDateTime.now().minusMonths(1).toInstant().truncatedTo(ChronoUnit.SECONDS)
  def monthsAgo(numMonths: Int): Timestamp      = new Timestamp(new LocalDate().toDateTimeAtStartOfDay.minusDays(numMonths * 31).getMillis)
  //TODO delete
  def twentyYearsAgo: Instant = ZonedDateTime.now().minusYears(20).toInstant().truncatedTo(ChronoUnit.SECONDS)
  def eighteenYearsAgo: Instant = ZonedDateTime.now().minusYears(18).toInstant().truncatedTo(ChronoUnit.SECONDS)
  val sdf = new java.text.SimpleDateFormat("MM/dd/yy HH:mm:ss")
  val mdf = new java.text.SimpleDateFormat("MMMM dd, yyyy")
  val dayMultiplier: Long =  24 * 60 * 60 * 1000L
  val hourMultiplier: Long = 60 * 60 * 1000L
}