package domain;

import cats.effect.kernel.Resource
import com.github.nscala_time.time.Imports.*
import io.circe.Decoder
import io.circe.generic.semiauto._
import io.circe.syntax._
import cats.syntax.functor._

type MediaStreamId = Int

/**
 * A MediaStream is an entity that represents a data stream flowing from a sink to a source, which is intended to 
 * eventually be integrated and maintained within the hospital system.
 * @tparam F
 */
trait MediaStream[F[_]] {
  val id: MediaStreamId
  val startDateTime: DateTime
  val stopDateTime: DateTime
  val source: MediaSource
  val sink: MediaSink

  def act: Resource[F, StreamingProcess[F]]
}

sealed trait MediaSource

object MediaSource {
  implicit val decoder: Decoder[MediaSource] =
    List[Decoder[MediaSource]](
      Decoder[RtmpSource].widen,
      Decoder[RtspSource].widen
    ).reduceLeft(_ or _)
}

case class RtmpSource private (url: String) extends MediaSource

object RtmpSource {
  implicit val decoder: Decoder[RtmpSource] = deriveDecoder[RtmpSource]

  def apply(url: String): RtmpSource = new RtmpSource(url)
}

case class RtspSource private (url: String)  extends MediaSource

object RtspSource {
  implicit val decoder: Decoder[RtspSource] = deriveDecoder[RtspSource]

  def apply(url: String): RtspSource = new RtspSource(url)
}

trait MediaSink

object MediaSink {
  implicit val decoder: Decoder[MediaSink] =
    List[Decoder[MediaSink]](
      Decoder[RtmpSink].widen,
      Decoder[HlsSink].widen
    ).reduceLeft(_ or _)
}

case class RtmpSink private (url: String) extends MediaSink {}

object RtmpSink {
  implicit val decoder: Decoder[RtmpSink] = deriveDecoder[RtmpSink]

  def apply(url: String): RtmpSink = new RtmpSink(url)
}

case class HlsSink private (path: String) extends MediaSink {}

object HlsSink {
  implicit val decoder: Decoder[HlsSink] = deriveDecoder[HlsSink]

  def apply(path: String): HlsSink = new HlsSink(path)
}

case class MediaStreamImpl[F[_] : StreamingBackend](
                                                     id: MediaStreamId,
                                                     startDateTime: DateTime,
                                                     stopDateTime: DateTime,
                                                     source: MediaSource,
                                                     sink: MediaSink,
                                                   ) extends MediaStream[F] {

  override def act: Resource[F, StreamingProcess[F]] = implicitly[StreamingBackend[F]].spawnNewProcess(source, sink)
}
