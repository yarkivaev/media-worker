package domain.server.streaming

import cats.Functor
import cats.effect.Sync
import domain.{MediaSink, MediaSource}

import scala.sys.process.*

class FFMpegStreamingBackend[F[_] : Sync : RunProcess : Functor]
(
  using sourceStreamingResource: StreamingResource[MediaSource],
  sinkStreamingResource: StreamingResource[MediaSink]
) extends StreamingBackend[F] {
  override def stream(mediaSource: MediaSource, mediaSink: MediaSink): F[Unit] = {
    val command =
      Seq("ffmpeg")
        ++ sourceStreamingResource.options(mediaSource).toList.flatMap((key, value) => List(key, value))
        ++ Seq("-i", sourceStreamingResource.destination(mediaSource))
        ++ sinkStreamingResource.options(mediaSink).toList.flatMap((key, value) => List(key, value))
        ++ Seq(sinkStreamingResource.destination(mediaSink))
    RunProcess[F]().run(
      command,
      ProcessLogger(line => {
        //      println(line)
      })
    ).use_
  }
}