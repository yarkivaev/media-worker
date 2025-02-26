package domain.streaming

import domain.temporal.TemporalObject
import domain.*

type FFMpeg[A]

object FFMpeg {
  given mediaSourceStreamingResource: StreamingResource[FFMpeg, MediaSource] =
    new StreamingResource[FFMpeg, MediaSource] {

      override def destination(media: MediaSource): String = media match
        case RtmpSource(url) => url
        case RtspSource(url) => url

      override def options(media: MediaSource): Map[String, String] = media match
        case RtmpSource(url) => Map()
        case RtspSource(url) => Map()
    }


  given mediaSinkStreamingResource[F[_], K[_]](
                                          using temporalStorage: TemporalObject[F, K, MediaSink]
                                        ): StreamingResource[FFMpeg, MediaSink] =
    new StreamingResource[FFMpeg, MediaSink] {

      override def destination(media: MediaSink): String = media match
        case RtmpSink(url) => url
        case HlsSink() => temporalStorage.path(media)

      override def options(media: MediaSink): Map[String, String] = media match
        case RtmpSink(url) => Map("-c" -> "copy", "-f" -> "flv")
        case HlsSink() => Map(
          "-c:v" -> "libx264",
          "-c:a" -> "acc",
          "-f" -> "hls",
          "-hls_time" -> "10",
          "-hls_list_size" -> "0",
          "-hls_segment_filename" -> "segment_%03d.ts"
        )
    }

}