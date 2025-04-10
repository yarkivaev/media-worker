package domain

import cats.effect.kernel.Clock
import cats.effect.kernel.Async
import cats.effect.{Spawn, Sync}
import cats.implicits.*
import domain.server.persistence.{FolderName, Storage}
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.{Codec, Decoder, Encoder}
import os.*

import scala.concurrent.duration.*
import cats.MonadError

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
}

/** Represents rtmp sink in hospital system
  *
  * @param url
  *   rtmp url address
  */
case class RtmpSink(url: String) extends MediaSink {}

object RtmpSink {
  given Codec[RtmpSink] = deriveCodec[RtmpSink]

  given [F[_]]: Storage[F, MediaSink] =
    throw new UnsupportedOperationException("Rtmp stream can not be saved to any storage")
}

type SinkName = String

/** Represents hls sink in hospital system
  *
  * @param sinkName
  *   name of the sink
  */
case class HlsSink(sinkName: SinkName) extends MediaSink {}

object HlsSink {
  given Codec[HlsSink] = deriveCodec[HlsSink]

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
}
