package medwork

import cats.effect._
import cats.effect.unsafe.implicits.global
import cats.implicits._
import com.comcast.ip4s.Port
import fs2.Pure
import fs2.Stream
import lepus.client._
import lepus.protocol.domains.ExchangeName
import lepus.protocol.domains.QueueName
import lepus.protocol.domains.ShortString
import lepus.std.ChannelCodec
import medwork.command.MediaWorkerCommand
import medwork.command.StartMediaStream
import medwork.command.StopMediaStream
import org.scalatest._
import org.scalatestplus.mockito.MockitoSugar
import org.testcontainers.containers.RabbitMQContainer

class BrokerSpec extends flatspec.AnyFlatSpec with MockitoSugar with BeforeAndAfterAll {

  val rabbitMQ: RabbitMQContainer = new RabbitMQContainer("rabbitmq:3-management")
    .withExposedPorts(5672, 15672) // 5672 for AMQP, 15672 for Web UI

  override def beforeAll(): Unit = {
    super.beforeAll()
    rabbitMQ.start()
  }

  override def afterAll(): Unit = {
    rabbitMQ.stop()
    super.afterAll()
  }

  "mediaSink" should "be able to put new MediaWorkerCommands" in {
    val stream: Stream[IO, MediaWorkerCommand] = Stream(
      StartMediaStream(
        RtmpSource("url"),
        RtmpSink("url")
      ),
      StopMediaStream(
        RtmpSource("url"),
        HlsSink("140")
      ),
      StartMediaStream(
        RtmpSource("url"),
        RtmpSink("url")
      )
    )
    val publisher = (for {
      queueClient <- LepusClient[IO](port = Port.fromInt(rabbitMQ.getAmqpPort).get)
      messageSink <- Broker.messageSink[IO, MediaWorkerCommand](queueClient, QueueName("queueName"))
    } yield messageSink)
      .use(pipe => pipe(stream).compile.drain)

    publisher.unsafeRunSync()
  }

  it should "be able to transfer MediaWorkerCommand" in {
    val stream: Stream[Pure, MediaWorkerCommand] = Stream(
      StartMediaStream(
        RtmpSource("url"),
        RtmpSink("url")
      ),
      StopMediaStream(
        RtmpSource("url"),
        HlsSink("140")
      ),
      StartMediaStream(
        RtmpSource("url"),
        RtmpSink("url")
      )
    )

    val queueName = QueueName("mediaworkercommand")

    implicit val channelCodec: ChannelCodec[String] = ChannelCodec.plain[String]

    val brokerClient = LepusClient[IO](port = Port.fromInt(rabbitMQ.getAmqpPort).get)

    val subscribe: Stream[IO, MediaWorkerCommand] = Stream
      .resource(brokerClient)
      .flatMap(Broker.messageSource[IO, MediaWorkerCommand](_, queueName))
      .flatMap(message => {
        Stream.eval(message.ack).map(_ => message.content)
      })

    val publish: Stream[IO, Unit] = stream
      .covary[IO]
      .flatMap(word =>
        Stream.eval((brokerClient.flatMap(_.channel).use { channel =>
          channel.queue.declare(queueName, durable = true) *>
            channel.messaging.publish(ExchangeName(""), queueName, word)
        }))
      )

    val streamedData = publish.zipRight(subscribe).take(3).compile.toList.unsafeRunSync()

    assert(streamedData === stream.toList)
  }
}
