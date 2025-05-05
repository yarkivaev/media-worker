package medwork.server.streaming

import cats.Functor
import cats.effect.Async
import cats.implicits._
import medwork.MediaSink
import medwork.MediaSource

import scala.sys.process._

class FFMpegStreamingBackend[F[_]: Async: RunProcess: Functor](using
  sourceStreamingResource: StreamingResource[F, MediaSource],
  sinkStreamingResource: StreamingResource[F, MediaSink]
) extends StreamingBackend[F] {
  override def stream(mediaSource: MediaSource, mediaSink: MediaSink): F[Unit] = {
    for {
      sourceDestination <- sourceStreamingResource.destination(mediaSource).map(Seq("-i", _))
      sourceOptionsMap <- sourceStreamingResource.options(mediaSource)
      sourceOptions = sourceOptionsMap.toList.flatMap((key, value) => List(key, value))
      sinkDestination <- sinkStreamingResource.destination(mediaSink).map(Seq(_))
      sinkOptionsMap <- sinkStreamingResource.options(mediaSink)
      sinkOptions = sinkOptionsMap.toList.flatMap((key, value) => List(key, value))
      command: ProcessBuilder = Seq("ffmpeg") ++ sourceOptions ++ sourceDestination ++ sinkOptions ++ sinkDestination
      _ <- summon[RunProcess[F]]
        .run(
          command,
          ProcessLogger(line => {
            println(line)
          })
        )
        .useForever
    } yield ()
  }
}
