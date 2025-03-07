package domain.client

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import com.comcast.ip4s.Port
import domain.Broker
import domain.command.MediaWorkerCommand
import lepus.client.LepusClient
import lepus.protocol.domains.QueueName

trait JavaClient {
  def executeCommand(command: MediaWorkerCommand): Unit
}

object JavaClient {
  def apply: JavaClient = {
    val messageSinkResource = for {
      lepusClient <- LepusClient[IO](port = Port.fromInt(5672).get)
      messageSink <- Broker.messageSink[IO, MediaWorkerCommand](
        lepusClient,
        QueueName("mediaworkercommand")
      )
    } yield messageSink

    new JavaClient {
      def executeCommand(command: MediaWorkerCommand): Unit = messageSinkResource.use(messageSink =>
        Client(messageSink).executeCommand(command)
      ).unsafeRunSync()
    }
  }
}
