package medwork.command

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import medwork.MediaSink
import medwork.MediaSource

final case class StartMediaStream(
  source: MediaSource,
  sink: MediaSink
) extends MediaWorkerCommand {
  self =>

  override def toJson: Json = self.asJson
}

object StartMediaStream {
  given Encoder[StartMediaStream] = (rv: StartMediaStream) =>
    Json.obj(
      "command" -> "StartMediaStream".asJson,
      "source" -> rv.source.asJson,
      "sink" -> rv.sink.asJson
    )

  given Decoder[StartMediaStream] = (c: HCursor) =>
    for {
      _ <- c.downField("command").as[String].flatMap {
        case "StartMediaStream" => Right(())
        case other              => Left(DecodingFailure(s"Unexpected type: $other", c.history))
      }
      source <- c.downField("source").as[MediaSource]
      middleware <- c.downField("sink").as[MediaSink]
    } yield StartMediaStream(source, middleware)
}
