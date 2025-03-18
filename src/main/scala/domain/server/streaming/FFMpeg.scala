package domain.server.streaming

import cats.effect.kernel.Clock
import domain.*
import domain.server.persistence.FolderName

object FFMpeg {
  given StreamingResource[MediaSource] =
    new StreamingResource[MediaSource] {

      override def destination(media: MediaSource): String = media match
        case RtmpSource(url) => url
        case RtspSource(url) => url

      override def options(media: MediaSource): Map[String, String] = media match
        case RtmpSource(url) => Map()
        case RtspSource(url) => Map()
    }

  given [F[_]: Clock](using folderName: FolderName[HlsSink]): StreamingResource[MediaSink] =
    new StreamingResource[MediaSink] {

      override def destination(media: MediaSink): String = media match
        case RtmpSink(url)     => url
        case HlsSink(sinkName) => s"$folderName/output.m3u8"

      override def options(media: MediaSink): Map[String, String] = media match
        case RtmpSink(url) => Map("-c" -> "copy", "-f" -> "flv")
        case HlsSink(sinkName) =>
          Map(
            "-c:v" -> "libx264",
            "-c:a" -> "acc",
            "-f" -> "hls",
            "-hls_time" -> "10",
            "-hls_list_size" -> "0",
            "-hls_segment_filename" -> s"$folderName/segment_%03d.ts"
          )
    }

}
