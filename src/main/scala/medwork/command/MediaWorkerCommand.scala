package medwork.command
import cats.Monad
import cats.implicits._
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import lepus.client.Message
import lepus.client.MessageDecoder
import lepus.client.MessageEncoder
import lepus.client.MessageRaw
import lepus.std.ChannelCodec
import medwork.MediaSink
import medwork.MediaSource
import medwork.MediaStream
import medwork.server.ActiveMediaStreams

/** Command for media worker to execute some action
  */
trait MediaWorkerCommand {
  self =>

  val source: MediaSource

  val sink: MediaSink

  def toJson: Json

  def execute[F[_]: ActiveMediaStreams: Monad]: F[Unit] = {
    summon[ActiveMediaStreams[F]]
      .manageMediaStream(
        MediaStream(source, sink)
      )
      .map(_ => ())
  }
}

object MediaWorkerCommand {
  given Encoder[MediaWorkerCommand] = Encoder.instance(_.toJson)

  given (using
    decoders: Map[String, Decoder[_ <: MediaWorkerCommand]]
  ): Decoder[MediaWorkerCommand] = Decoder.instance { cursor =>
    cursor.get[String]("command").flatMap { typeName =>
      decoders.get(typeName) match {
        case Some(decoder) => decoder.tryDecode(cursor)
        case None          => Left(DecodingFailure(s"Unknown MediaWorkerCommand type: $typeName", cursor.history))
      }
    }
  }

  given Map[String, Decoder[_ <: MediaWorkerCommand]] = Map(
    "StartMediaStream" -> summon[Decoder[StartMediaStream]],
    "StopMediaStream" -> summon[Decoder[StopMediaStream]]
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
