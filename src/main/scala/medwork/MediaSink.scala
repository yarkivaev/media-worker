package medwork
import cats.MonadError
import cats.effect.Spawn
import cats.effect.Sync
import cats.effect.kernel.Async
import cats.implicits._
import cats.kernel.Hash
import io.circe.Codec
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.Encoder
import io.circe.Json
import io.circe.generic.semiauto._
import io.circe.syntax._
import medwork.server.persistence.FolderName
import medwork.server.persistence.Storage
import os._

import scala.concurrent.duration._

/** Represents sink of a media flow in a hospital system.
  */
sealed trait MediaSink

object MediaSink {
  given Decoder[MediaSink] =
    List[Decoder[MediaSink]](
      Decoder[RtmpSink].widen,
      Decoder[HlsSink].widen
    ).reduceLeft(_ or _)

  given Encoder[MediaSink] =
    Encoder.instance {
      case rtmp: RtmpSink => rtmp.asJson
      case hls: HlsSink   => hls.asJson
    }

  import HlsSink.given

  given [F[_]: Async](using
    fileStorage: Storage[F, Path],
    folderName: FolderName[F, HlsSink],
    monadError: MonadError[F, Throwable]
  ): Storage[F, MediaSink] = {
    case RtmpSink(url) => monadError.raiseError(RuntimeException("Can not save rtmp stream"))
    case sink: HlsSink => summon[Storage[F, HlsSink]].save(sink)
  }

  given Hash[MediaSink] = Hash.by {
    case rtmpSink: RtmpSink => rtmpSink.hash
    case hlsSink: HlsSink   => hlsSink.hash
  }
}

/** Represents rtmp sink in hospital system
  *
  * @param url
  *   rtmp url address
  */
final case class RtmpSink(url: String) extends MediaSink {}

object RtmpSink {
  given encoder: Encoder[RtmpSink] = Encoder.instance { u =>
    Json.obj(
      "type" -> "RtmpSink".asJson,
      "url" -> u.url.asJson
    )
  }
  given decoder: Decoder[RtmpSink] = Decoder.instance { cursor =>
    for {
      _ <- cursor.downField("type").as[String].flatMap {
        case "RtmpSink" => Right(())
        case other      => Left(DecodingFailure(s"Unexpected type: $other", cursor.history))
      }
      url <- cursor.downField("url").as[String]
    } yield RtmpSink(url)
  }
  given Codec[RtmpSink] = Codec.from(decoder, encoder)

  given [F[_]]: Storage[F, MediaSink] =
    throw new UnsupportedOperationException("Rtmp stream can not be saved to any storage")

  given Hash[RtmpSink] = Hash.by(rtmpSink => ("RtmpSink", rtmpSink.url))
}

type SinkName = String

/** Represents hls sink in hospital system
  *
  * @param sinkName
  *   name of the sink
  */
final case class HlsSink(sinkName: SinkName) extends MediaSink {}

object HlsSink {
  given encoder: Encoder[HlsSink] = Encoder.instance { u =>
    Json.obj(
      "type" -> "HlsSink".asJson,
      "sinkName" -> u.sinkName.asJson
    )
  }
  given decoder: Decoder[HlsSink] = Decoder.instance { cursor =>
    for {
      _ <- cursor.downField("type").as[String].flatMap {
        case "HlsSink" => Right(())
        case other     => Left(DecodingFailure(s"Unexpected type: $other", cursor.history))
      }
      url <- cursor.downField("sinkName").as[String]
    } yield HlsSink(url)
  }
  given Codec[HlsSink] = Codec.from(decoder, encoder)

  given [F[_]: Sync](using sinkName: Name[HlsSink]): FolderName[F, HlsSink] =
    hlsSink => {
      val folderPath = os.pwd / sinkName(hlsSink)
      for {
        _ <- Sync[F].delay {
          if (!os.exists(folderPath)) {
            os.makeDir.all(folderPath)
          }
        }
      } yield sinkName(hlsSink)
    }

  given [F[_]: Async](using fileStorage: Storage[F, Path], folderName: FolderName[F, HlsSink]): Storage[F, HlsSink] = {
    def doFrequently[A](work: F[A]): F[A] = Sync[F].fix[A](loop =>
      for {
        res <- work
        _ <- Async[F].sleep(1.second)
        _ <- loop
      } yield res
    )

    hlsSink =>
      for {
        name <- folderName(hlsSink)
        _ <- Spawn[F]
          .both[Unit, Unit](
            doFrequently(
              {
                val filePath = os.pwd / name / "output.m3u8"

                Sync[F].delay(os.exists(filePath)).flatMap { exists =>
                  if (exists) Sync[F].delay(filePath).flatMap(fileStorage.save)
                  else Sync[F].unit
                }
              }
            ),
            doFrequently(
              for {
                paths <- Sync[F].delay(
                  os.walk(os.pwd / name)
                    .filter(_.last.startsWith("segment"))
                    .toList
                )
                _ <- paths.map(fileStorage.save).sequence
                _ <- Sync[F].delay(paths.foreach(os.remove))
              } yield ()
            )
          )
      } yield ()
  }

  given Hash[HlsSink] = Hash.by(hlsSink => ("HlsSink", hlsSink.sinkName))
}
