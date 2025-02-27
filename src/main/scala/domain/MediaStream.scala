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
  given Decoder[MediaSource] =
    List[Decoder[MediaSource]](
      Decoder[RtmpSource].widen,
      Decoder[RtspSource].widen
    ).reduceLeft(_ or _)

  given Encoder[MediaSource] =
    Encoder.instance {
      case rtmp: RtmpSource => rtmp.asJson
      case rtsp: RtspSource => rtsp.asJson
    }
}

case class RtmpSource(url: String) extends MediaSource

object RtmpSource {
  given Codec[RtmpSource] = deriveCodec[RtmpSource]
}

case class RtspSource(url: String)  extends MediaSource

object RtspSource {
  given Codec[RtspSource] = deriveCodec[RtspSource]
}

sealed trait MediaSink

object MediaSink {
  given Decoder[MediaSink] =
    List[Decoder[MediaSink]](
      Decoder[RtmpSink].widen,
      Decoder[HlsSink].widen
    ).reduceLeft(_ or _)

  given Encoder[MediaSink] =
    Encoder.instance {
      case rtmp: RtmpSink => rtmp.asJson
      case hls: HlsSink => hls.asJson
    }
}

case class RtmpSink(url: String) extends MediaSink {}

object RtmpSink {
  given Codec[RtmpSink] = deriveCodec[RtmpSink]
}


type SinkName = String

case class HlsSink(sinkName: SinkName) extends MediaSink {}

object HlsSink {
  given Codec[HlsSink] = deriveCodec[HlsSink]
  
}

case class MediaStreamImpl[F[_] : StreamingBackend](
                                                     id: MediaStreamId,
                                                     startDateTime: DateTime,
                                                     stopDateTime: DateTime,
                                                     source: MediaSource,
                                                     sink: MediaSink,
                                                   ) extends MediaStream[F] {

  override def act: F[Unit] = summon[StreamingBackend[F]].stream(source, sink)
}
