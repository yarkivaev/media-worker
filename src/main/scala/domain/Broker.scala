package domain

import cats.effect.kernel.MonadCancel
import cats.effect.{Concurrent, Resource}
import cats.implicits.*
import cats.{Functor, Monad}
import fs2.{Pipe, Stream}
import io.circe.Decoder
import io.circe.parser.decode
import lepus.client.*
import lepus.protocol.domains.*

/** Represents message, obtained from broker queue. Provides ability to acknowledge message or reject it.
  * @tparam F
  *   Runtime
  * @tparam T
  *   Message content type
  */
trait BrokerMessage[F[_], T] {

  /** Message content
    */
  val content: T

  /** Acknowledge message.
    * @return
    *   Effect that acknowledges message
    */
  def ack: F[Unit]

  /** Not acknowledge message.
    * @return
    *   Effect that rejects message
    */
  def nack: F[Unit]
}

object BrokerMessage {
  given [F[_]: Functor]: Functor[[A] =>> BrokerMessage[F, A]] with
    def map[A, B](fa: BrokerMessage[F, A])(f: A => B): BrokerMessage[F, B] =
      new {
        val content: B = f(fa.content)

        def ack: F[Unit] = fa.ack

        def nack: F[Unit] = fa.nack
      }
}

object Broker {

  /** Source of messages from broker queue.
    *
    * Provides effectful streams of broker messages. Decodes message content using provided decoder.
    * @param con
    *   RabbitMQ connection
    * @param queueName
    *   queue name
    * @param monadCancel
    *   monadCancel
    * @param decoder
    *   content decoder
    * @tparam F
    *   Effect
    * @tparam A
    *   content type
    * @return
    *   Stream of broker messages
    */
  def messageSource[F[_], A](con: Connection[F], queueName: QueueName)(using
    monadCancel: MonadCancel[F, Throwable],
    decoder: Decoder[A]
  ): Stream[F, BrokerMessage[F, A]] = {
    for {
      brokerMessage <- messageSourceRaw(con, queueName)
      decodedEither = brokerMessage.map(raw => decode[A](raw))
      decoded <- decodedEither.content match {
        case Left(e) => Stream.eval(decodedEither.nack) >> Stream.empty
        case Right(decodedMessage) =>
          Stream.emit[F, BrokerMessage[F, A]](new {
            val content: A = decodedMessage

            def ack: F[Unit] = decodedEither.ack

            def nack: F[Unit] = decodedEither.nack
          })
      }
    } yield decoded
  }

  /** Source of messages from broker queue.
    *
    * Provides effectful streams of broker messages.
    * @param con
    *   RabbitMQ connection
    * @param queueName
    *   queue name
    * @param monadCancel
    *   monadCancel
    * @param decoder
    *   content decoder
    * @tparam F
    *   Effect
    * @return
    *   Stream of broker messages
    */
  private def messageSourceRaw[F[_]](con: Connection[F], queueName: QueueName)(using
    monadCancel: MonadCancel[F, Throwable],
    decoder: MessageDecoder[String]
  ): Stream[F, BrokerMessage[F, String]] =
    for {
      channel <- Stream.resource(con.channel)
      _ <- Stream.eval(channel.queue.declare(queueName, durable = true))
      deliveredMessage <- channel.messaging
        .consumeRaw(queueName, noAck = false)
    } yield new BrokerMessage[F, String] {
      val content: String = decoder.decode(deliveredMessage.message).right.get.payload

      def ack: F[Unit] = channel.messaging.ack(deliveredMessage.deliveryTag)

      def nack: F[Unit] = channel.messaging.nack(deliveredMessage.deliveryTag)
    }

  /** Sink that sends data to broker queue
    * @param con
    *   RabbitMQ connection
    * @param queueName
    *   queue name
    * @param monadCancel
    *   monadCancel
    * @tparam F
    *   Effect
    * @tparam A
    *   Content type
    * @return
    *   Resource with function that forwards data stream to broker queue
    */
  def messageSink[F[_]: Concurrent: Monad, A: MessageEncoder](con: Connection[F], queueName: QueueName)(using
    monadCancel: MonadCancel[F, Throwable]
  ): Resource[F, Pipe[F, A, ReturnedMessageRaw]] =
    (for {
      channel <- con.channel
      _ <- Resource.eval(channel.queue.declare(queueName, durable = true))
      publisher <- Resource.pure(channel.messaging.publisher[A])
    } yield publisher)
      .map(sink =>
        messages =>
          sink(
            messages.map(command =>
              Envelope(
                ExchangeName(""),
                queueName,
                true,
                Message(command)
              )
            )
          )
      )
}
