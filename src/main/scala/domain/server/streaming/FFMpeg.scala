package domain.server.streaming

import cats.implicits._
import cats.effect.kernel.Clock
import domain.*
import domain.server.persistence.FolderName
import cats.Applicative

object FFMpeg {
  given [F[_]: Applicative]: StreamingResource[F, MediaSource] =
    new StreamingResource[F, MediaSource] {

      override def destination(media: MediaSource): F[String] = Applicative[F].pure(
        media match {
          case RtmpSource(url) => url
          case RtspSource(url) => url
        }
      )

      override def options(media: MediaSource): F[Map[String, String]] = Applicative[F].pure(
        media match {
          case RtmpSource(url) => Map()
          case RtspSource(url) => Map()
        }
      )
    }

  given [F[_]: Clock: Applicative](using folderName: FolderName[F, HlsSink]): StreamingResource[F, MediaSink] =
    new StreamingResource[F, MediaSink] {

      override def destination(media: MediaSink): F[String] = media match
        case RtmpSink(url)    => Applicative[F].pure(url)
        case hlsSink: HlsSink => folderName(hlsSink).map(folderName => s"$folderName/output.m3u8")

      override def options(media: MediaSink): F[Map[String, String]] = media match
        case RtmpSink(url) => Applicative[F].pure(Map("-c" -> "copy", "-f" -> "flv"))
        case hlsSink: HlsSink =>
          for {
            name <- folderName(hlsSink)
          } yield Map(
            "-c:v" -> "libx264",
            "-preset" -> "fast",
            "-c:a" -> "aac",
            "-max_muxing_queue_size" -> "1024",
            "-f" -> "hls",
            "-hls_time" -> "10",
            "-hls_list_size" -> "0",
            "-hls_segment_filename" -> s"$name/segment_%03d.ts"
          )
    }

}
