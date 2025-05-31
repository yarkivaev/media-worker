package medwork.command

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import medwork.MediaSink
import medwork.MediaSource

final case class StopMediaStream(source: MediaSource, sink: MediaSink) extends MediaWorkerCommand {
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
