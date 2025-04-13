package medwork.command

import cats.effect.kernel.MonadCancel
import cats.effect.{Async, Sync, Spawn}
import cats.implicits.*
import medwork.server.ActiveMediaStreams
import medwork.server.persistence.Storage
import medwork.server.streaming.StreamingBackend
import medwork.{MediaSink, MediaSource, MediaStream}
import io.circe.*
import io.circe.syntax.*

case class SaveStream(source: MediaSource, mediaSink: MediaSink) extends MediaWorkerCommand {
  self =>

  override def toJson: Json = self.asJson
  
  override def act[F[_]: Async: StreamingBackend: ActiveMediaStreams](using
    Storage[F, MediaSink],
    MonadCancel[F, Throwable]
  ): F[Unit] =
    summon[ActiveMediaStreams[F]].manageMediaStream(
      MediaStream(source, mediaSink),
      Spawn[F]
        .both(
          summon[StreamingBackend[F]].stream(source, mediaSink),
          summon[Storage[F, MediaSink]].save(mediaSink)
        )
        .map(_ => ())
    )
}

object SaveStream {
  given Encoder[SaveStream] = (rv: SaveStream) =>
    Json.obj(
      "source" -> rv.source.asJson,
      "mediaSink" -> rv.mediaSink.asJson
    )

  given Decoder[SaveStream] = (c: HCursor) =>
    for {
      source <- c.downField("source").as[MediaSource]
      hlsSink <- c.downField("mediaSink").as[MediaSink]
    } yield SaveStream(source, hlsSink)
}
