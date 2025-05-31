package medwork.client

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import medwork.command.MediaWorkerCommand

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
