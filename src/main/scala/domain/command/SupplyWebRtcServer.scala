package domain.command

import cats.effect.kernel.{MonadCancel, Sync}
import cats.effect.{Async, Spawn}
import cats.implicits.*
import cats.syntax.*
import domain.server.ActiveMediaStreams
import domain.server.persistence.Storage
import domain.server.streaming.StreamingBackend
import domain.{MediaSink, MediaSource, MediaStream}
import io.circe.*
import io.circe.syntax.*


case class SupplyWebRtcServer(
                               source: MediaSource,
                               webRtc: MediaSink
                             )
  extends MediaWorkerCommand {

  override def act[F[_] : Async : StreamingBackend : ActiveMediaStreams]
  (using Storage[F, MediaSink], MonadCancel[F, Throwable]): F[Unit] =
    Sync[F].pure(println("helloSupply"))
    >>
    summon[ActiveMediaStreams[F]].manageMediaStream(
      MediaStream(source, webRtc),
      summon[StreamingBackend[F]].stream(source, webRtc)
    )
}

object SupplyWebRtcServer {
  given Encoder[SupplyWebRtcServer] = (rv: SupplyWebRtcServer) => Json.obj(
    "source" -> rv.source.asJson,
    "webRtc" -> rv.webRtc.asJson
  )

  given Decoder[SupplyWebRtcServer] = (c: HCursor) => for {
    source <- c.downField("source").as[MediaSource]
    webRtc <- c.downField("webRtc").as[MediaSink]
  } yield SupplyWebRtcServer(source, webRtc)
}