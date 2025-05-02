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
  sink: MediaSink
) extends MediaWorkerCommand {
  self =>

  override def toJson: Json = self.asJson
}

object RedirectStream {
  given Encoder[RedirectStream] = (rv: RedirectStream) =>
    Json.obj(
      "type" -> "RedirectStream".asJson,
      "source" -> rv.source.asJson,
      "sink" -> rv.sink.asJson
    )

  given Decoder[RedirectStream] = (c: HCursor) =>
    for {
      source <- c.downField("source").as[MediaSource]
      middleware <- c.downField("sink").as[MediaSink]
    } yield RedirectStream(source, middleware)
}
