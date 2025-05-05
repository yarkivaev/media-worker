package medwork

import cats.implicits._
import cats.instances.HashInstances._
import cats.kernel.Hash
import io.circe.Codec
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.Encoder
import io.circe.Json
import io.circe.syntax._

/** Represents source of a media flow in a hospital system.
  */
sealed trait MediaSource

object MediaSource {
  given Decoder[MediaSource] =
    List[Decoder[MediaSource]](
      Decoder[RtmpSource].widen,
      Decoder[RtspSource].widen
    ).reduceLeft(_ or _)

  given Encoder[MediaSource] =
    Encoder.instance {
      case rtmp: RtmpSource => rtmp.asJson
      case rtsp: RtspSource => rtsp.asJson
    }

  given Hash[MediaSource] = Hash.by {
    case rtmpSource: RtmpSource => rtmpSource.hash
    case rtspSource: RtspSource => rtspSource.hash
  }
}

/** Represents rtmp source in hospital system
  *
  * @param url
  *   rtmp url address
  */
final case class RtmpSource(url: String) extends MediaSource

object RtmpSource {
  given encoder: Encoder[RtmpSource] = Encoder.instance { u =>
    Json.obj(
      "type" -> "RtmpSource".asJson,
      "url" -> u.url.asJson
    )
  }
  given decoder: Decoder[RtmpSource] = Decoder.instance { cursor =>
    for {
      _ <- cursor.downField("type").as[String].flatMap {
        case "RtmpSource" => Right(())
        case other        => Left(DecodingFailure(s"Unexpected type: $other", cursor.history))
      }
      url <- cursor.downField("url").as[String]
    } yield RtmpSource(url)
  }
  given Codec[RtmpSource] = Codec.from(decoder, encoder)

  given Hash[RtmpSource] = Hash.by(rtmpSource => ("RtmpSource", rtmpSource.url))
}

/** Represents rtsp source in hospital system
  *
  * @param url
  *   rtsp url address
  */
final case class RtspSource(url: String) extends MediaSource

object RtspSource {
  given encoder: Encoder[RtspSource] = Encoder.instance { u =>
    Json.obj(
      "type" -> "RtspSource".asJson,
      "url" -> u.url.asJson
    )
  }
  given decoder: Decoder[RtspSource] = Decoder.instance { cursor =>
    for {
      _ <- cursor.downField("type").as[String].flatMap {
        case "RtspSource" => Right(())
        case other        => Left(DecodingFailure(s"Unexpected type: $other", cursor.history))
      }
      url <- cursor.downField("url").as[String]
    } yield RtspSource(url)
  }
  given Codec[RtspSource] = Codec.from(decoder, encoder)

  given Hash[RtspSource] = Hash.by(rtspSource => ("RtspSource", rtspSource.url))
}
