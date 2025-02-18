package domain;

import cats.effect.kernel.Resource
import com.github.nscala_time.time.Imports.*

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
  val goal: MediaStreamType
  val source: MediaSource
  val sink: MediaSink

  def act: Resource[F, StreamingProcess[F]]
}

trait MediaStreamType {}

case class RecordVideoSource() extends MediaStreamType {}

case class CameraToMiddleware() extends MediaStreamType {}

case class SupplyWebRtcServer() extends MediaStreamType {}

trait MediaSource {
  val url: String
}

case class RtmpSource(url: String) extends MediaSource {}

case class RtspSource(url: String) extends MediaSource {}

trait MediaSink {
  val url: String
}

case class RtmpSink(url: String) extends MediaSink {}

case class HlsSink(url: String) extends MediaSink {}

case class MediaStreamImpl[F[_] : StreamingBackend](
                                                     id: MediaStreamId,
                                                     startDateTime: DateTime,
                                                     stopDateTime: DateTime,
                                                     goal: MediaStreamType,
                                                     source: MediaSource,
                                                     sink: MediaSink,
                                                   ) extends MediaStream[F] {

  override def act: Resource[F, StreamingProcess[F]] = implicitly[StreamingBackend[F]].spawnNewProcess(source, sink)
}
