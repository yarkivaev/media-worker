package domain.command

import cats.effect.Spawn
import cats.effect.kernel.MonadCancel
import cats.implicits.*
import domain.MediaSink
import domain.command.{RecordVideoSource, RouteCameraToMiddleware, SupplyWebRtcServer}
import domain.server.ActiveMediaStreams
import domain.server.persistence.Storage
import domain.server.streaming.StreamingBackend
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import lepus.client.{Message, MessageDecoder, MessageEncoder, MessageRaw}
import lepus.std.ChannelCodec
import io.circe.Encoder.encodeString


trait MediaWorkerCommand {
  def act[F[_] : Spawn : StreamingBackend : ActiveMediaStreams]
  (using Storage[F, MediaSink], MonadCancel[F, Throwable]): F[Unit]
}

object MediaWorkerCommand {
  given Encoder[MediaWorkerCommand] = Encoder.instance {
    case record: RecordVideoSource => record.asJson
    case route: RouteCameraToMiddleware => route.asJson
    case supplyWebRtc: SupplyWebRtcServer => supplyWebRtc.asJson
  }

  given (using Decoder[RecordVideoSource], Decoder[RouteCameraToMiddleware], Decoder[SupplyWebRtcServer])
  : Decoder[MediaWorkerCommand] =
    List[Decoder[MediaWorkerCommand]](
      Decoder[RecordVideoSource].widen,
      Decoder[RouteCameraToMiddleware].widen,
      Decoder[SupplyWebRtcServer].widen
    ).reduceLeft(_ or _)

  given (using encoder: Encoder[MediaWorkerCommand], decoder: Decoder[MediaWorkerCommand])
  : Codec[MediaWorkerCommand] = Codec.from(decoder, encoder)

  given ChannelCodec[String] = ChannelCodec.plain[String]

  given (using Decoder[MediaWorkerCommand]): ChannelCodec[MediaWorkerCommand] =
    new ChannelCodec[MediaWorkerCommand] {
      override def encode(msg: Message[MediaWorkerCommand]): Either[Throwable, MessageRaw] =
        summon[ChannelCodec[String]].encode(msg.payload.asJson.noSpaces)

      override def decode(msg: MessageRaw): Either[Throwable, Message[MediaWorkerCommand]] =
        for {
          message <- summon[ChannelCodec[String]].decode(msg)
          message <- parse(message.payload).map(message.withPayload)
          message <- message.payload.as[MediaWorkerCommand].map(message.withPayload)
        } yield message
    }

  given MessageEncoder[MediaWorkerCommand] = msg =>
    MessageEncoder[String].encode(msg.payload.asJson.noSpaces)
    
  given (using channelCodec: ChannelCodec[MediaWorkerCommand]): MessageDecoder[MediaWorkerCommand] = raw => 
    channelCodec.decode(raw)
}
