package domain;

import cats.effect.kernel.Resource
import com.github.nscala_time.time.Imports.*
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import cats.syntax.functor.*

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

  def act: F[Unit]
}

sealed trait MediaSource

object MediaSource {
  implicit val decoder: Decoder[MediaSource] =
    List[Decoder[MediaSource]](
      Decoder[RtmpSource].widen,
      Decoder[RtspSource].widen
    ).reduceLeft(_ or _)

  implicit val encoder: Encoder[MediaSource] =
    Encoder.instance {
      case rtmp: RtmpSource => rtmp.asJson
      case rtsp: RtspSource => rtsp.asJson
    }
}

case class RtmpSource private (url: String) extends MediaSource

object RtmpSource {
  implicit val decoder: Codec[RtmpSource] = deriveCodec[RtmpSource]

  def apply(url: String): RtmpSource = new RtmpSource(url)
}

case class RtspSource private (url: String)  extends MediaSource

object RtspSource {
  implicit val decoder: Codec[RtspSource] = deriveCodec[RtspSource]

  def apply(url: String): RtspSource = new RtspSource(url)
}

trait MediaSink

object MediaSink {
  implicit val decoder: Decoder[MediaSink] =
    List[Decoder[MediaSink]](
      Decoder[RtmpSink].widen,
      Decoder[HlsSink].widen
    ).reduceLeft(_ or _)

  implicit val encoder: Encoder[MediaSink] =
    Encoder.instance {
      case rtmp: RtmpSink => rtmp.asJson
      case hls: HlsSink => hls.asJson
    }
}

case class RtmpSink private (url: String) extends MediaSink {}

object RtmpSink {
  implicit val decoder: Codec[RtmpSink] = deriveCodec[RtmpSink]

  def apply(url: String): RtmpSink = new RtmpSink(url)
}

case class HlsSink private (path: String) extends MediaSink {}

object HlsSink {
  implicit val decoder: Codec[HlsSink] = deriveCodec[HlsSink]

  def apply(path: String): HlsSink = new HlsSink(path)
}

case class MediaStreamImpl[F[_] : StreamingBackend](
                                                     id: MediaStreamId,
                                                     startDateTime: DateTime,
                                                     stopDateTime: DateTime,
                                                     source: MediaSource,
                                                     sink: MediaSink,
                                                   ) extends MediaStream[F] {

  override def act: F[Unit] = implicitly[StreamingBackend[F]].stream(source, sink)
}
