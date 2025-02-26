package domain

import cats.Monad
import cats.effect.kernel.MonadCancel
import cats.implicits.*
import domain.streaming.StreamingBackend
import domain.temporal.TemporalObject
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import lepus.client.{Message, MessageEncoder, MessageRaw}
import lepus.std.ChannelCodec


sealed trait MediaWorkerCommand[F[_]] {
  def act: F[Unit]
}

case class RecordVideoSource[F[_] : Monad, K[_]](source: MediaSource, hlsSink: MediaSink)
                                                (
                                                  using streamingBackend: StreamingBackend[F],
                                                  temporalObject: TemporalObject[F, K, MediaSink],
                                                  monadCancel: MonadCancel[F, Throwable]
                                                )
  extends MediaWorkerCommand[F] {
  override def act: F[Unit] =
    for {
      _ <- streamingBackend.stream(source, hlsSink)
      _ <- temporalObject.save
    } yield ()
}

case class RouteCameraToMiddleware[F[_]](
                                          camera: MediaSource,
                                          middleware: MediaSink
                                        )
                                        (
                                          using streamingBackend: StreamingBackend[F],
                                          monadCancel: MonadCancel[F, Throwable]
                                        )
  extends MediaWorkerCommand[F] {

  override def act: F[Unit] =
    streamingBackend.stream(camera, middleware)
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

object MediaWorkerCommand {
  given codec[F[_]]: Codec[MediaWorkerCommand[F]] = ??? //deriveCodec[MediaWorkerCommand[F]]

  given channelStringCodec: ChannelCodec[String] = ChannelCodec.plain[String]

  given channelCodec[F[_]]: ChannelCodec[MediaWorkerCommand[F]] = new ChannelCodec[MediaWorkerCommand[F]]:
    override def encode(msg: Message[MediaWorkerCommand[F]]): Either[Throwable, MessageRaw] =
      channelStringCodec.encode(msg.payload.asJson.noSpaces)

    override def decode(msg: MessageRaw): Either[Throwable, Message[MediaWorkerCommand[F]]] =
      for {
        message <- channelStringCodec.decode(msg)
        message <- parse(message.payload).map(message.withPayload)
        message <- message.payload.as[MediaWorkerCommand[F]].map(message.withPayload)
      } yield message

  given messageEncoder[F[_]]: MessageEncoder[MediaWorkerCommand[F]] = msg =>
    MessageEncoder[String].encode(msg.payload.asJson.noSpaces)
}
