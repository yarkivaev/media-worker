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

case class SaveStream(source: MediaSource, sink: MediaSink) extends MediaWorkerCommand {
  self =>

  override def toJson: Json = self.asJson
}

object SaveStream {
  given Encoder[SaveStream] = (rv: SaveStream) =>
    Json.obj(
      "type" -> "SaveStream".asJson,
      "source" -> rv.source.asJson,
      "sink" -> rv.sink.asJson
    )

  given Decoder[SaveStream] = (c: HCursor) =>
    for {
      source <- c.downField("source").as[MediaSource]
      hlsSink <- c.downField("sink").as[MediaSink]
    } yield SaveStream(source, hlsSink)
}
