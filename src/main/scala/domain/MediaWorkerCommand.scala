package domain

import cats.effect.kernel.MonadCancel
import cats.implicits.*
import lepus.client.{Message, MessageEncoder, MessageRaw}
import lepus.std.ChannelCodec
import io.circe.*
import io.circe.generic.auto.*
import io.circe.generic.semiauto.deriveCodec
import io.circe.parser.*
import io.circe.syntax.*


sealed trait MediaWorkerCommand {
  def act[F[_]: StreamingBackend](using monadCancel: MonadCancel[F, Throwable]): F[Unit]
}

case class RecordVideoSource(source: MediaSource) extends MediaWorkerCommand {
  def act[F[_]](using streamingBackend: StreamingBackend[F], monadCancel: MonadCancel[F, Throwable]): F[Unit] =
    streamingBackend.stream(source, ???)
}

case class RouteCameraToMiddleware(
                                    camera: MediaSource,
                                    middleware: MediaSink
                                  ) extends MediaWorkerCommand {

  def act[F[_]](using streamingBackend: StreamingBackend[F], monadCancel: MonadCancel[F, Throwable]): F[Unit] =
    streamingBackend.stream(camera, middleware)
}

case class SupplyWebRtcServer(
                               source: MediaSource,
                               webRtc: MediaSink
                             ) extends MediaWorkerCommand {

  def act[F[_]](using streamingBackend: StreamingBackend[F], monadCancel: MonadCancel[F, Throwable]): F[Unit] =
    streamingBackend.stream(source, webRtc)
}

object MediaWorkerCommand {
  implicit val codec: Codec[MediaWorkerCommand] = deriveCodec[MediaWorkerCommand]

  implicit val channelStringCodec: ChannelCodec[String] = ChannelCodec.plain[String]

  implicit val channelCodec: ChannelCodec[MediaWorkerCommand] = new ChannelCodec[MediaWorkerCommand]:
    override def encode(msg: Message[MediaWorkerCommand]): Either[Throwable, MessageRaw] =
      channelStringCodec.encode(msg.payload.asJson.noSpaces)

    override def decode(msg: MessageRaw): Either[Throwable, Message[MediaWorkerCommand]] =
      for {
        message <- channelStringCodec.decode(msg)
        message <- parse(message.payload).map(message.withPayload)
        message <- message.payload.as[MediaWorkerCommand].map(message.withPayload)
      } yield message

  implicit val messageEncoder: MessageEncoder[MediaWorkerCommand] = msg => 
    MessageEncoder[String].encode(msg.payload.asJson.noSpaces)
}
