package domain.command

import cats.effect.kernel.MonadCancel
import domain.server.ActiveMediaStreams
import domain.server.streaming.StreamingBackend
import domain.{MediaSink, MediaSource, MediaStream, MediaWorkerCommand}
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*


case class SupplyWebRtcServer[F[_]](
                                     source: MediaSource,
                                     webRtc: MediaSink
                                   )
                                   (
                                     using streamingBackend: StreamingBackend[F],
                                     monadCancel: MonadCancel[F, Throwable],
                                     activeMediaStreams: ActiveMediaStreams[F]
                                   )
  extends MediaWorkerCommand[F] {

  override def act: F[Unit] =
    activeMediaStreams.manageMediaStream(
      MediaStream(source, webRtc),
      streamingBackend.stream(source, webRtc)
    )
}

object SupplyWebRtcServer {
  given [F[_]]: Encoder[SupplyWebRtcServer[F]] = (rv: SupplyWebRtcServer[F]) => Json.obj(
    "source" -> rv.source.asJson,
    "webRtc" -> rv.webRtc.asJson
  )

  given [F[_] : StreamingBackend : ActiveMediaStreams]
  (using monadCancel: MonadCancel[F, Throwable])
  : Decoder[SupplyWebRtcServer[F]] = (c: HCursor) => for {
    source <- c.downField("source").as[MediaSource]
    webRtc <- c.downField("webRtc").as[MediaSink]
  } yield SupplyWebRtcServer(source, webRtc)
}