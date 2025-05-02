package medwork

import cats.implicits.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.{Codec, Decoder, Encoder}
import cats.kernel.Hash
import cats.instances.HashInstances._
import cats.syntax._
import io.circe.Json
import io.circe.DecodingFailure

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

  given Hash[MediaSource] = Hash.by{
    case rtmpSource: RtmpSource => rtmpSource.hash
    case rtspSource: RtspSource => rtspSource.hash
  }
}

/** Represents rtmp source in hospital system
  *
  * @param url
  *   rtmp url address
  */
case class RtmpSource(url: String) extends MediaSource

object RtmpSource {
  given encoder: Encoder[RtmpSource] = Encoder.instance { u =>
    Json.obj(
      "type" -> "RtmpSource".asJson,
      "url" -> u.url.asJson,
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
case class RtspSource(url: String) extends MediaSource

object RtspSource {
  given encoder: Encoder[RtspSource] = Encoder.instance { u =>
    Json.obj(
      "type" -> "RtspSource".asJson,
      "url" -> u.url.asJson,
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
