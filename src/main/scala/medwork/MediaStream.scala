package medwork;

import io.circe.generic.semiauto.*
import cats.effect.{Async, Spawn}
import medwork.server.streaming.StreamingBackend
import medwork.server.persistence.Storage
import cats.effect.kernel.MonadCancel
import cats.implicits._
import cats.kernel.Hash
import cats.instances.HashInstances._
import cats.syntax._

/** Represents media data flow in the hospital system.
  *
  * @param source
  *   Source of the flow.
  * @param sink
  *   Sink of the flow.
  */
case class MediaStream(source: MediaSource, sink: MediaSink) {
  /** executes command action
    * @tparam F
    *   Effect
    * @return
    *   Effect that process command action
    */
  def act[F[_]: Async: StreamingBackend](using
    Storage[F, MediaSink],
    MonadCancel[F, Throwable]
  ): F[Unit] = (source, sink) match {
    case (source, storableSink: HlsSink) => 
      Spawn[F]
        .both(
          summon[StreamingBackend[F]].stream(source, sink),
          summon[Storage[F, MediaSink]].save(sink)
        )
        .map(_ => ())
    case (source, sink) => summon[StreamingBackend[F]].stream(source, sink)
  }
}

object MediaStream {
  given Hash[MediaStream] = Hash.by(mediaStream => (mediaStream.source, mediaStream.sink))

  // implicit val mediaStreamCodec: MongoCodec[MediaStream] = MongoCodec.derive[MediaStream]
  // https://www.mongodb.com/docs/drivers/java/sync/current/data-formats/codecs/
  // https://github.com/Kirill5k/mongo4cats/blob/474ff1e6ca5804c783933f3e8bbfe7e4d89f25b7/modules/core/src/main/scala/mongo4cats/database/MongoDatabase.scala
  // https://www.mongodb.com/docs/drivers/java/sync/current/data-formats/codecs/#std-label-codecs-custom-example
}
