package domain.client

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import com.comcast.ip4s.Port
import domain.Broker
import domain.command.MediaWorkerCommand
import lepus.client.LepusClient
import lepus.protocol.domains.QueueName

/** Accessible from Java client
  */
trait JavaClient {
  def executeCommand(command: MediaWorkerCommand): Unit
}

object JavaClient {
  def apply(queuePort: Int): JavaClient = {
    new JavaClient {
      def executeCommand(command: MediaWorkerCommand): Unit =
        Client(queuePort).use(client => client.executeCommand(command)).unsafeRunSync()
    }
  }
}
