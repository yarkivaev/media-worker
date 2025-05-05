package medwork.command

import medwork.{MediaSink, MediaSource, MediaStream}
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*

case class StopMediaStream(source: MediaSource, sink: MediaSink) extends MediaWorkerCommand {
  self =>

  override def toJson: Json = self.asJson
}

object StopMediaStream {
  given Encoder[StopMediaStream] = (rv: StopMediaStream) =>
    Json.obj(
      "command" -> "StopMediaStream".asJson,
      "source" -> rv.source.asJson,
      "sink" -> rv.sink.asJson
    )

  given Decoder[StopMediaStream] = (c: HCursor) =>
    for {
      source <- c.downField("source").as[MediaSource]
      hlsSink <- c.downField("sink").as[MediaSink]
    } yield StopMediaStream(source, hlsSink)
}
