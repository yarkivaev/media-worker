package domain

import cats.implicits.*
import domain.command.{RecordVideoSource, RouteCameraToMiddleware, SupplyWebRtcServer}
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import lepus.client.{Message, MessageEncoder, MessageRaw}
import lepus.std.ChannelCodec


trait MediaWorkerCommand[F[_]] {
  def act: F[Unit]
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
