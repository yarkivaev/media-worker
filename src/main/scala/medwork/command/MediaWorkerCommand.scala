package medwork.command

import cats.effect.Async
import cats.effect.kernel.MonadCancel
import cats.implicits.*
import medwork.MediaSink
import medwork.server.ActiveMediaStreams
import medwork.server.persistence.Storage
import medwork.server.streaming.StreamingBackend
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import lepus.client.{Message, MessageDecoder, MessageEncoder, MessageRaw}
import lepus.std.ChannelCodec

/** Command for media worker to execute some action
  */
trait MediaWorkerCommand {

  def toJson: Json

  /** executes command action
    * @tparam F
    *   Effect
    * @return
    *   Effect that process command action
    */
  def act[F[_]: Async: StreamingBackend: ActiveMediaStreams](using
    Storage[F, MediaSink],
    MonadCancel[F, Throwable]
  ): F[Unit]
}

object MediaWorkerCommand {
  given Encoder[MediaWorkerCommand] = Encoder.instance(_.toJson)

  given (using
    decoders: List[Decoder[_ <: MediaWorkerCommand]]
  ): Decoder[MediaWorkerCommand] =
    decoders.map(_.widen).reduceLeft(_ or _)

  given List[Decoder[_ <: MediaWorkerCommand]] = List(
    summon[Decoder[RedirectStream]],
    summon[Decoder[SaveStream]]
  )

  given (using encoder: Encoder[MediaWorkerCommand], decoder: Decoder[MediaWorkerCommand]): Codec[MediaWorkerCommand] =
    Codec.from(decoder, encoder)

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

  given MessageEncoder[MediaWorkerCommand] = msg => MessageEncoder[String].encode(msg.payload.asJson.noSpaces)

  given (using channelCodec: ChannelCodec[MediaWorkerCommand]): MessageDecoder[MediaWorkerCommand] = raw =>
    channelCodec.decode(raw)
}
