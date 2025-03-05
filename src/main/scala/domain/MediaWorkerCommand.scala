package domain

import cats.effect.Spawn
import cats.effect.kernel.MonadCancel
import cats.implicits.*
import domain.server.persistence.Storage
import domain.server.streaming.StreamingBackend
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import lepus.client.{Message, MessageEncoder, MessageRaw}
import lepus.std.ChannelCodec


sealed trait MediaWorkerCommand[F[_]] {
  def act: F[Unit]
}

case class RecordVideoSource[F[_] : Spawn](source: MediaSource, mediaSink: MediaSink)
                                          (
                                            using streamingBackend: StreamingBackend[F],
                                            storage: Storage[F, MediaSink],
                                            monadCancel: MonadCancel[F, Throwable]
                                          )
  extends MediaWorkerCommand[F] {
  override def act: F[Unit] =
    Spawn[F].both(
      streamingBackend.stream(source, mediaSink),
      storage.save(mediaSink)
    ).map(_ => ())
}

object RecordVideoSource {
  given [F[_]]: Encoder[RecordVideoSource[F]] = (rv: RecordVideoSource[F]) => Json.obj(
    "source" -> rv.source.asJson,
    "mediaSink" -> rv.mediaSink.asJson
  )

  given [F[_] : StreamingBackend : Spawn]
  (using temporal: Storage[F, MediaSink], monadCancel: MonadCancel[F, Throwable])
  : Decoder[RecordVideoSource[F]] = (c: HCursor) => for {
    source <- c.downField("source").as[MediaSource]
    hlsSink <- c.downField("mediaSink").as[MediaSink]
  } yield RecordVideoSource(source, hlsSink)
}

case class RouteCameraToMiddleware[F[_]](
                                          source: MediaSource,
                                          middleware: MediaSink
                                        )
                                        (
                                          using streamingBackend: StreamingBackend[F],
                                          monadCancel: MonadCancel[F, Throwable]
                                        )
  extends MediaWorkerCommand[F] {

  override def act: F[Unit] =
    streamingBackend.stream(source, middleware)
}

object RouteCameraToMiddleware {
  given [F[_]]: Encoder[RouteCameraToMiddleware[F]] = (rv: RouteCameraToMiddleware[F]) => Json.obj(
    "source" -> rv.source.asJson,
    "middleware" -> rv.middleware.asJson
  )

  given [F[_] : StreamingBackend]
  (using monadCancel: MonadCancel[F, Throwable])
  : Decoder[RouteCameraToMiddleware[F]] = (c: HCursor) => for {
    source <- c.downField("source").as[MediaSource]
    middleware <- c.downField("middleware").as[MediaSink]
  } yield RouteCameraToMiddleware(source, middleware)
}

case class SupplyWebRtcServer[F[_]](
                                     source: MediaSource,
                                     webRtc: MediaSink
                                   )
                                   (
                                     using streamingBackend: StreamingBackend[F],
                                     monadCancel: MonadCancel[F, Throwable]
                                   )
  extends MediaWorkerCommand[F] {

  override def act: F[Unit] =
    streamingBackend.stream(source, webRtc)
}

object SupplyWebRtcServer {
  given [F[_]]: Encoder[SupplyWebRtcServer[F]] = (rv: SupplyWebRtcServer[F]) => Json.obj(
    "source" -> rv.source.asJson,
    "webRtc" -> rv.webRtc.asJson
  )

  given [F[_] : StreamingBackend]
  (using monadCancel: MonadCancel[F, Throwable])
  : Decoder[SupplyWebRtcServer[F]] = (c: HCursor) => for {
    source <- c.downField("source").as[MediaSource]
    webRtc <- c.downField("webRtc").as[MediaSink]
  } yield SupplyWebRtcServer(source, webRtc)
}

object MediaWorkerCommand {
  given [F[_]]: Encoder[MediaWorkerCommand[F]] = Encoder.instance {
    case record: RecordVideoSource[F] => record.asJson
    case route: RouteCameraToMiddleware[F] => route.asJson
    case supplyWebRtc: SupplyWebRtcServer[F] => supplyWebRtc.asJson
  }

  given [F[_]](using Decoder[RecordVideoSource[F]], Decoder[RouteCameraToMiddleware[F]], Decoder[SupplyWebRtcServer[F]])
  : Decoder[MediaWorkerCommand[F]] =
    List[Decoder[MediaWorkerCommand[F]]](
      Decoder[RecordVideoSource[F]].widen,
      Decoder[RouteCameraToMiddleware[F]].widen,
      Decoder[SupplyWebRtcServer[F]].widen
    ).reduceLeft(_ or _)

  given [F[_]](using encoder: Encoder[MediaWorkerCommand[F]], decoder: Decoder[MediaWorkerCommand[F]])
  : Codec[MediaWorkerCommand[F]] = Codec.from(decoder, encoder)

  given ChannelCodec[String] = ChannelCodec.plain[String]

  given [F[_]](using Decoder[MediaWorkerCommand[F]]): ChannelCodec[MediaWorkerCommand[F]] =
    new ChannelCodec[MediaWorkerCommand[F]] {
      override def encode(msg: Message[MediaWorkerCommand[F]]): Either[Throwable, MessageRaw] =
        summon[ChannelCodec[String]].encode(msg.payload.asJson.noSpaces)

      override def decode(msg: MessageRaw): Either[Throwable, Message[MediaWorkerCommand[F]]] =
        for {
          message <- summon[ChannelCodec[String]].decode(msg)
          message <- parse(message.payload).map(message.withPayload)
          message <- message.payload.as[MediaWorkerCommand[F]].map(message.withPayload)
        } yield message
    }

  given [F[_]]: MessageEncoder[MediaWorkerCommand[F]] = msg =>
    MessageEncoder[String].encode(msg.payload.asJson.noSpaces)
}
