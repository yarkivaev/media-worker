package domain

import cats.Monad
import cats.effect.kernel.MonadCancel
import cats.effect.{Concurrent, Resource}
import fs2.{Pipe, Stream}
import lepus.client.*
import lepus.protocol.domains.*
import lepus.std.*

trait BrokerMessage[F[_], T] {
  val message: T

  def ack: F[Unit]
}

object Broker {
  def messageSource[F[_], A]
  (con: Connection[F], queueName: QueueName)
  (using monadCancel: MonadCancel[F, Throwable], channelCodec: ChannelCodec[A])
  : Stream[F, BrokerMessage[F, A]] =
    for {
      channel <- Stream.resource(con.channel)
      _ <- Stream.eval(channel.queue.declare(queueName, durable = true))
      source <- channel.messaging
        .consumeRaw(queueName, noAck = false)
        .flatMap(env =>
          channelCodec
            .decode(env.message)
            .fold(
              _ => Stream.exec(channel.messaging.reject(env.deliveryTag, false)),
              msg => Stream.emit(new BrokerMessage[F, A] {
                val message: A = msg.payload

                def ack: F[Unit] = channel.messaging.ack(env.deliveryTag)
              })
            )
        )
    } yield source

  def messageSink[F[_] : Concurrent : Monad, A: MessageEncoder]
  (con: Connection[F], queueName: QueueName)
  (using monadCancel: MonadCancel[F, Throwable]): Resource[F, Pipe[F, Envelope[A], ReturnedMessageRaw]] =
    for {
      channel <- con.channel
      publisher <- Resource.pure(channel.messaging.publisher[A])
    } yield publisher
}