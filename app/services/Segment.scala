package services

import com.segment.analytics.Analytics
import com.segment.analytics.messages.{AliasMessage, IdentifyMessage, TrackMessage}
import play.api.Configuration

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._


class Segment(
  val configuration: Configuration,
  implicit val ec: ExecutionContext
) extends Service {

  val secretKey: String = configuration.get[String]("segment.api.key")

  val analytics: Analytics = Analytics.builder(secretKey).build()

  def identify(userId: UUID, properties: Map[String, Any]): Unit = {
    analytics.enqueue(IdentifyMessage.builder()
      .userId(userId.toString)
      .traits(properties.asJava))
  }

  def track(name: String, userId: UUID, maybeProperties: Option[Map[String, Any]] = None): Unit = {
    maybeProperties match {
      case Some(properties) =>
        analytics.enqueue(TrackMessage.builder(name)
          .userId(userId.toString)
          .properties(properties.asJava))
      case None =>
         analytics.enqueue(TrackMessage.builder(name)
            .userId(userId.toString))
    }
  }

  def registerUser(waitlistUserId: UUID, userId: UUID, dob: Long) = {
    analytics.enqueue(AliasMessage.builder(waitlistUserId.toString)
      .userId(userId.toString))
  }
}
