package domain.command

import cats.effect.kernel.MonadCancel
import domain.server.ActiveMediaStreams
import domain.server.streaming.StreamingBackend
import domain.{MediaSink, MediaSource, MediaStream, MediaWorkerCommand}
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*


case class RouteCameraToMiddleware[F[_]](
                                          source: MediaSource,
                                          middleware: MediaSink
                                        )
                                        (
                                          using streamingBackend: StreamingBackend[F],
                                          monadCancel: MonadCancel[F, Throwable],
                                          activeMediaStreams: ActiveMediaStreams[F]
                                        )
  extends MediaWorkerCommand[F] {

  override def act: F[Unit] =
    activeMediaStreams.manageMediaStream(
      MediaStream(source, middleware),
      streamingBackend.stream(source, middleware)
    )
}

object RouteCameraToMiddleware {
  given [F[_]]: Encoder[RouteCameraToMiddleware[F]] = (rv: RouteCameraToMiddleware[F]) => Json.obj(
    "source" -> rv.source.asJson,
    "middleware" -> rv.middleware.asJson
  )

  given [F[_] : StreamingBackend : ActiveMediaStreams]
  (using monadCancel: MonadCancel[F, Throwable])
  : Decoder[RouteCameraToMiddleware[F]] = (c: HCursor) => for {
    source <- c.downField("source").as[MediaSource]
    middleware <- c.downField("middleware").as[MediaSink]
  } yield RouteCameraToMiddleware(source, middleware)
}