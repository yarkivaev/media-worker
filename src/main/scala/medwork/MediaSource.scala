package medwork

import cats.implicits.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.{Codec, Decoder, Encoder}

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
}

/** Represents rtmp source in hospital system
  *
  * @param url
  *   rtmp url address
  */
case class RtmpSource(url: String) extends MediaSource

object RtmpSource {
  given Codec[RtmpSource] = deriveCodec[RtmpSource]
}

/** Represents rtsp source in hospital system
  *
  * @param url
  *   rtsp url address
  */
case class RtspSource(url: String) extends MediaSource

object RtspSource {
  given Codec[RtspSource] = deriveCodec[RtspSource]
}
