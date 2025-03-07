package domain.command

import cats.Applicative
import cats.effect.Spawn
import cats.effect.kernel.MonadCancel
import domain.server.ActiveMediaStreams
import domain.server.persistence.Storage
import domain.server.streaming.StreamingBackend
import domain.{MediaSink, MediaSource, MediaStream}
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import cats.syntax._
import cats.implicits._


case class SupplyWebRtcServer(
                               source: MediaSource,
                               webRtc: MediaSink
                             )
  extends MediaWorkerCommand {

  override def act[F[_] : Spawn : StreamingBackend : ActiveMediaStreams]
  (using Storage[F, MediaSink], MonadCancel[F, Throwable]): F[Unit] =
    Applicative[F].pure(println("hello")) 
//      *>
//    summon[ActiveMediaStreams[F]].manageMediaStream(
//      MediaStream(source, webRtc),
//      summon[StreamingBackend[F]].stream(source, webRtc)
//    )
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