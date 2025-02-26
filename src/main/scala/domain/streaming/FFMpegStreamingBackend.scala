package domain.streaming

import cats.effect.Sync
import domain.{MediaSink, MediaSource}

import scala.sys.process.*

class FFMpegStreamingBackend[F[_] : Sync, G[_], K[_]]
(
  using sourceStreamingResource: StreamingResource[G, MediaSource],
  sinkStreamingResource: StreamingResource[G, MediaSink]
) extends StreamingBackend[F] {
  override def stream(mediaSource: MediaSource, mediaSink: MediaSink): F[Unit] = {
    val command =
      Seq("ffmpeg")
        ++ sourceStreamingResource.options(mediaSource).toList.flatMap((key, value) => List(key, value))
        ++ Seq("-i", sourceStreamingResource.destination(mediaSource))
        ++ sinkStreamingResource.options(mediaSink).toList.flatMap((key, value) => List(key, value))
        ++ Seq(sinkStreamingResource.destination(mediaSink))
    Sync[F].delay({
      val process = command.run(ProcessLogger(line => {
        //      println(line)
      }))
      process.exitValue()
    })
  }
}