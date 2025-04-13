package medwork.command

import cats.effect.Async
import cats.effect.kernel.MonadCancel
import medwork.server.ActiveMediaStreams
import medwork.server.persistence.Storage
import medwork.server.streaming.StreamingBackend
import medwork.{MediaSink, MediaSource, MediaStream}
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*

case class RedirectStream(
  source: MediaSource,
  middleware: MediaSink
) extends MediaWorkerCommand {
  self =>

  override def toJson: Json = self.asJson

  override def act[F[_]: Async: StreamingBackend: ActiveMediaStreams](using
    Storage[F, MediaSink],
    MonadCancel[F, Throwable]
  ): F[Unit] =
    summon[ActiveMediaStreams[F]].manageMediaStream(
      MediaStream(source, middleware),
      summon[StreamingBackend[F]].stream(source, middleware)
    )
}

object RedirectStream {
  given Encoder[RedirectStream] = (rv: RedirectStream) =>
    Json.obj(
      "source" -> rv.source.asJson,
      "middleware" -> rv.middleware.asJson
    )

  given Decoder[RedirectStream] = (c: HCursor) =>
    for {
      source <- c.downField("source").as[MediaSource]
      middleware <- c.downField("middleware").as[MediaSink]
    } yield RedirectStream(source, middleware)
}
