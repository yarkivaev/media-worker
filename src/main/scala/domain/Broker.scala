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

trait BrokerMessage[F[_], T] {
  val message: T

  def ack: F[Unit]

  def nack: F[Unit]
}

object BrokerMessage {
  given [F[_]: Functor]: Functor[[A] =>> BrokerMessage[F, A]] with
    def map[A, B](fa: BrokerMessage[F, A])(f: A => B): BrokerMessage[F, B] =
      new {
        val message: B = f(fa.message)

        def ack: F[Unit] = fa.ack

        def nack: F[Unit] = fa.nack
      }
}

object Broker {

  def messageSource[F[_], A](con: Connection[F], queueName: QueueName)(using
    monadCancel: MonadCancel[F, Throwable],
    decoder: Decoder[A]
  ): Stream[F, BrokerMessage[F, A]] = {
    for {
      brokerMessage <- messageSourceRaw(con, queueName)
      decodedEither = brokerMessage.map(raw => decode[A](raw))
      decoded <- decodedEither.message match {
        case Left(e) => Stream.eval(decodedEither.nack) >> Stream.empty
        case Right(decodedMessage) =>
          Stream.emit[F, BrokerMessage[F, A]](new {
            val message: A = decodedMessage

            def ack: F[Unit] = decodedEither.ack

            def nack: F[Unit] = decodedEither.nack
          })
      }
    } yield decoded
  }

  def messageSourceRaw[F[_]](con: Connection[F], queueName: QueueName)(using
    monadCancel: MonadCancel[F, Throwable],
    decoder: MessageDecoder[String]
  ): Stream[F, BrokerMessage[F, String]] =
    for {
      channel <- Stream.resource(con.channel)
      _ <- Stream.eval(channel.queue.declare(queueName, durable = true))
      deliveredMessage <- channel.messaging
        .consumeRaw(queueName, noAck = false)
    } yield new BrokerMessage[F, String] {
      val message: String = decoder.decode(deliveredMessage.message).right.get.payload

      def ack: F[Unit] = channel.messaging.ack(deliveredMessage.deliveryTag)

      def nack: F[Unit] = channel.messaging.nack(deliveredMessage.deliveryTag)
    }

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

//Stream(new BrokerMessage[F, A] {
//  val message: A = (()).asInstanceOf[A]
//
//  def ack: F[Unit] = Applicative[F].pure(())
//})
//Stream.resource(con.channel).flatMap(ch => ch.messaging.consumeRaw(queueName, noAck = false).map( mes =>
//{
//  println(mes)
//  new BrokerMessage[F, A] {
//    val message: A = (()).asInstanceOf[A]
//
//    def ack: F[Unit] = Applicative[F].pure(())
//  }
//}
//))
