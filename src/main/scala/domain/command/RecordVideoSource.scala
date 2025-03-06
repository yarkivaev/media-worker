package domain.command

import cats.effect.Spawn
import cats.effect.kernel.MonadCancel
import cats.implicits.*
import domain.server.ActiveMediaStreams
import domain.server.persistence.Storage
import domain.server.streaming.StreamingBackend
import domain.{MediaSink, MediaSource, MediaStream, MediaWorkerCommand}
import io.circe.*
import io.circe.syntax.*

case class RecordVideoSource[F[_] : Spawn](source: MediaSource, mediaSink: MediaSink)
                                          (
                                            using streamingBackend: StreamingBackend[F],
                                            storage: Storage[F, MediaSink],
                                            monadCancel: MonadCancel[F, Throwable],
                                            activeMediaStreams: ActiveMediaStreams[F]
                                          )
  extends MediaWorkerCommand[F] {
  override def act: F[Unit] =
    activeMediaStreams.manageMediaStream(
      MediaStream(source, mediaSink),
      Spawn[F].both(
        streamingBackend.stream(source, mediaSink),
        storage.save(mediaSink)
      ).map(_ => ())
    )
}

object RecordVideoSource {
  given [F[_]]: Encoder[RecordVideoSource[F]] = (rv: RecordVideoSource[F]) => Json.obj(
    "source" -> rv.source.asJson,
    "mediaSink" -> rv.mediaSink.asJson
  )

  given [F[_] : StreamingBackend : Spawn]
  (using temporal: Storage[F, MediaSink], monadCancel: MonadCancel[F, Throwable],
   activeMediaStreams: ActiveMediaStreams[F])
  : Decoder[RecordVideoSource[F]] = (c: HCursor) => for {
    source <- c.downField("source").as[MediaSource]
    hlsSink <- c.downField("mediaSink").as[MediaSink]
  } yield RecordVideoSource(source, hlsSink)
}