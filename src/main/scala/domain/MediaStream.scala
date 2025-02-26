package domain;

import cats.effect.kernel.Resource
import com.github.nscala_time.time.Imports.*
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import cats.syntax.functor.*
import domain.streaming.StreamingBackend

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

case class RtmpSource(url: String) extends MediaSource

object RtmpSource {
  implicit val decoder: Codec[RtmpSource] = deriveCodec[RtmpSource]
}

case class RtspSource(url: String)  extends MediaSource

object RtspSource {
  implicit val decoder: Codec[RtspSource] = deriveCodec[RtspSource]
}

sealed trait MediaSink

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

case class RtmpSink(url: String) extends MediaSink {}

object RtmpSink {
  implicit val decoder: Codec[RtmpSink] = deriveCodec[RtmpSink]
}

case class HlsSink() extends MediaSink {}

object HlsSink {
  implicit val decoder: Codec[HlsSink] = deriveCodec[HlsSink]
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
