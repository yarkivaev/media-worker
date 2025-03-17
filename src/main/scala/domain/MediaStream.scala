package domain;

import cats.effect.kernel.Async
import cats.effect.{Spawn, Sync}
import cats.implicits.*
import com.github.nscala_time.time.Imports.DateTime
import domain.server.persistence.{FolderName, Storage}
import domain.server.streaming.StreamingBackend
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.{Codec, Decoder, Encoder}
import os.*

import scala.concurrent.duration.*

/**
 * A MediaStream is an entity that represents a data stream flowing from a sink to a source, which is intended to 
 * eventually be integrated and maintained within the hospital system.
 *
 * @tparam F
 */
case class MediaStream(source: MediaSource, sink: MediaSink)

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

case class RtmpSource(url: String) extends MediaSource

object RtmpSource {
  given Codec[RtmpSource] = deriveCodec[RtmpSource]
}

case class RtspSource(url: String) extends MediaSource

object RtspSource {
  given Codec[RtspSource] = deriveCodec[RtspSource]
}

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
      case hls: HlsSink => hls.asJson
    }

  given [F[_]]: Storage[F, MediaSink] = {
    case RtmpSink(url) => ???
    case HlsSink(sinkName) => ???
  }
}

case class RtmpSink(url: String) extends MediaSink {}

object RtmpSink {
  given Codec[RtmpSink] = deriveCodec[RtmpSink]

  given [F[_]]: Storage[F, MediaSink] =
    throw new UnsupportedOperationException("Rtmp stream can not be saved to any storage")
}


type SinkName = String

case class HlsSink(sinkName: SinkName) extends MediaSink {}

object HlsSink {
  given Codec[HlsSink] = deriveCodec[HlsSink]

  given [F[_] : Async](using fileStorage: Storage[F, Path], folderName: FolderName[HlsSink]): Storage[F, HlsSink] = {
    def doFrequently[A](work: F[A]): F[A] = Sync[F].fix[A](loop =>
      for {
        res <- work
        _ <- Async[F].sleep(1.second)
        _ <- loop
      } yield res
    )

    hlsSink =>
      Spawn[F].both[Unit, Unit](
        doFrequently(
          Sync[F].delay(os.pwd / folderName(hlsSink) / "output.m3u8")
            .flatMap(fileStorage.save)
        ),
        doFrequently(
          for {
            paths <- Sync[F].delay(
              os.walk(os.pwd / folderName(hlsSink))
                .filter(_.last.startsWith("segment"))
                .toList
            )
            _ <- paths.map(fileStorage.save).sequence
            _ <- Sync[F].delay(paths.foreach(os.remove))
          } yield ()
        )
      ).map(_ => ())
  }
}
