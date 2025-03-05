package domain

import cats.effect.*
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import com.comcast.ip4s.Port
import domain.server.persistence.Storage
import domain.server.streaming.StreamingBackendImpl
import fs2.{Pure, Stream}
import io.circe.*
import io.circe.parser.*
import lepus.client.{LepusClient, MessageEncoder}
import lepus.protocol.domains.{ExchangeName, QueueName}
import lepus.std.ChannelCodec
import org.scalatest.*
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

  implicit val codec: Codec[String] = Codec.from(Decoder.decodeString, Encoder.encodeString)

  private def streamSequence[T: ChannelCodec : MessageEncoder : Codec](stream: Stream[IO, T]): IO[List[T]] = {
    assert(rabbitMQ.isRunning)

    println(s"AMQP URL: ${rabbitMQ.getAmqpUrl}")
    println(s"Web UI: ${rabbitMQ.getHttpUrl}")

    println(s"${rabbitMQ.getAmqpPort}")

    val queueName = QueueName("mediaworkercommand")

    implicit val channelCodec: ChannelCodec[String] = ChannelCodec.plain[String]

    val brokerClient = LepusClient[IO](port = Port.fromInt(rabbitMQ.getAmqpPort).get)

    val subscribe: Stream[IO, String] = Stream.resource(brokerClient)
      .flatMap(Broker.messageSource(_, queueName)).flatMap(message => {
        Stream.eval(IO.println(message.message) *> message.ack).map(_ => message.message)
      })

    val publish: Stream[IO, Unit] = stream.covary[IO].flatMap(word =>
      Stream.eval(
        (brokerClient.flatMap(_.channel).use { channel =>
          IO.println(word) *>
            channel.queue.declare(queueName, durable = true) *>
            channel.messaging.publish(ExchangeName(""), queueName, word)
        })))

    publish.mergeHaltL(subscribe).compile.toList.map(list => list.filter(_ != ())
      .asInstanceOf[List[String]]).map(list => list.map(parse(_).toTry.get.as[T].toTry.get))
  }

  it should "be able to transfer MediaWorkerCommand" in {

    given StreamingBackendImpl[IO] = StreamingBackendImpl[IO]()

    given Storage[IO, MediaSink] = Storage.fake

    given Spawn[IO] = IO.asyncForIO

    val stream: Stream[Pure, MediaWorkerCommand[IO]] = Stream(
      RouteCameraToMiddleware(
        RtmpSource("url"),
        RtmpSink("url")
      ),
      RecordVideoSource(
        RtmpSource("url"),
        HlsSink("140")
      ),
      SupplyWebRtcServer(
        RtmpSource("url"),
        RtmpSink("url")
      )
    )


    val streamedData = streamSequence(stream).unsafeRunSync()

    println(streamedData)

    assert(streamedData == stream.toList)
  }
}
