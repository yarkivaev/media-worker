package domain.command

import cats.effect.Spawn
import cats.effect.kernel.MonadCancel
import domain.server.ActiveMediaStreams
import domain.server.persistence.Storage
import domain.server.streaming.StreamingBackend
import domain.{MediaSink, MediaSource, MediaStream}
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*


case class RouteCameraToMiddleware(
                                          source: MediaSource,
                                          middleware: MediaSink
                                        )
  extends MediaWorkerCommand {

  override def act[F[_] : Spawn : StreamingBackend : ActiveMediaStreams]
  (using Storage[F, MediaSink], MonadCancel[F, Throwable]): F[Unit] =
    summon[ActiveMediaStreams[F]].manageMediaStream(
      MediaStream(source, middleware),
      summon[StreamingBackend[F]].stream(source, middleware)
    )
}

object RouteCameraToMiddleware {
  given Encoder[RouteCameraToMiddleware] = (rv: RouteCameraToMiddleware) => Json.obj(
    "source" -> rv.source.asJson,
    "middleware" -> rv.middleware.asJson
  )

  given Decoder[RouteCameraToMiddleware] = (c: HCursor) => for {
    source <- c.downField("source").as[MediaSource]
    middleware <- c.downField("middleware").as[MediaSink]
  } yield RouteCameraToMiddleware(source, middleware)
}