package medwork.client

import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec
import org.scalatestplus.mockito.MockitoSugar
import org.testcontainers.containers.RabbitMQContainer

class ClientSpec extends flatspec.AnyFlatSpec with MockitoSugar with BeforeAndAfterAll {

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

  "client" should "be able to send MediaWorkerCommand" in {

    //    Client.apply(Broker.)
  }
}
