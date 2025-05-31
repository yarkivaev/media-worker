import com.dimafeng.testcontainers.{GenericContainer, RabbitMQContainer, MinIOContainer}
import org.testcontainers.utility.DockerImageName;
import com.dimafeng.testcontainers.lifecycle.and
import org.testcontainers.containers.Network
import org.testcontainers.containers.Network.newNetwork
import cats.effect.kernel.Resource
import cats.effect.IO
import medwork.client.Client

object Setup {
  type SetupType = GenericContainer and GenericContainer and RabbitMQContainer and GenericContainer and MinIOContainer

  def setup: SetupType = {
    val network: Network = newNetwork()
    val rtspServer = DummyRtspServer().configure(container => {
      container.setNetwork(network)
    })
    rtspServer.start()
    val rtspStream = DummyRtspStream(rtspServer.networkAliases.head, 8554).configure(container => {
      container.setNetwork(network)
    })
    rtspStream.start()
    val rabbitMQ = RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management")).configure(container => {
      container.setNetwork(network)
    })
    rabbitMQ.start()
    val s3 = MinIOContainer().configure(container => {
      container.setNetwork(network)
    })
    s3.start()
    val mediaWorker = MediaWorker(rabbitMQ.networkAliases.head, s3.networkAliases.head, 5672).configure(container => {
      container.setNetwork(network)
    })
    mediaWorker.start()
    rtspServer and rtspStream and rabbitMQ and mediaWorker and s3
  }

  def client: SetupType => Resource[IO, (medwork.client.Client[cats.effect.IO], RelatedContainers)] = containers => {
    val s3 = containers.tail
    val mediaWorker = containers.head.tail
    val rabbitMq: RabbitMQContainer = containers.head.head.tail
    val rtspServer: GenericContainer = containers.head.head.head.head
    val rtspStream: GenericContainer = containers.head.head.head.tail
    Client(rabbitMq.amqpPort)
      .map(
        (
          _,
          RelatedContainers(
            s3,
            mediaWorker,
            rabbitMq,
            rtspServer,
            rtspStream
          )
        )
      )
  }
}

case class RelatedContainers(
  s3: MinIOContainer,
  mediaWorker: GenericContainer,
  queue: RabbitMQContainer,
  rtspServer: GenericContainer,
  dummyStream: GenericContainer
)
