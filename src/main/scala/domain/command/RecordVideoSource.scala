package domain.command

import cats.effect.Spawn
import cats.effect.kernel.MonadCancel
import cats.implicits.*
import domain.server.ActiveMediaStreams
import domain.server.persistence.Storage
import domain.server.streaming.StreamingBackend
import domain.{MediaSink, MediaSource, MediaStream}
import io.circe.*
import io.circe.syntax.*

case class RecordVideoSource(source: MediaSource, mediaSink: MediaSink)
  extends MediaWorkerCommand {
  override def act[F[_] : Spawn : StreamingBackend : ActiveMediaStreams]
  (using Storage[F, MediaSink], MonadCancel[F, Throwable]): F[Unit] =
    summon[ActiveMediaStreams[F]].manageMediaStream(
      MediaStream(source, mediaSink),
      Spawn[F].both(
        summon[StreamingBackend[F]].stream(source, mediaSink),
        summon[Storage[F, MediaSink]].save(mediaSink)
      ).map(_ => ())
    )
}

object RecordVideoSource {
  given Encoder[RecordVideoSource] = (rv: RecordVideoSource) => Json.obj(
    "source" -> rv.source.asJson,
    "mediaSink" -> rv.mediaSink.asJson
  )

  given Decoder[RecordVideoSource] = (c: HCursor) => for {
    source <- c.downField("source").as[MediaSource]
    hlsSink <- c.downField("mediaSink").as[MediaSink]
  } yield RecordVideoSource(source, hlsSink)
}