package medwork.server.streaming
import cats.effect.Sync
import cats.effect.kernel.MonadCancel
import cats.effect.kernel.Resource

import scala.sys.process.Process
import scala.sys.process.ProcessBuilder
import scala.sys.process.ProcessIO
import scala.sys.process.ProcessLogger

/** Resource process wrapper
  * @tparam F
  *   Effect
  */
trait RunProcess[F[_]] {
  def run(processBuilder: ProcessBuilder): Resource[F, Process]

  def run(processBuilder: ProcessBuilder, log: ProcessLogger): Resource[F, Process]

  def run(processBuilder: ProcessBuilder, io: ProcessIO): Resource[F, Process]

  def run(processBuilder: ProcessBuilder, connectInput: Boolean): Resource[F, Process]

  def run(processBuilder: ProcessBuilder, log: ProcessLogger, connectInput: Boolean): Resource[F, Process]
}

object RunProcess {
  def apply[F[_]: Sync](using monadCancel: MonadCancel[F, Throwable]): RunProcess[F] = new RunProcess[F]:
    override def run(processBuilder: ProcessBuilder): Resource[F, Process] =
      Resource.make(Sync[F].delay(processBuilder.run()))(process => Sync[F].delay(process.destroy()))

    override def run(processBuilder: ProcessBuilder, log: ProcessLogger): Resource[F, Process] =
      Resource.make(Sync[F].delay(processBuilder.run(log)))(process => Sync[F].delay(process.destroy()))

    override def run(processBuilder: ProcessBuilder, io: ProcessIO): Resource[F, Process] =
      Resource.make(Sync[F].delay(processBuilder.run(io)))(process => Sync[F].delay(process.destroy()))

    override def run(processBuilder: ProcessBuilder, connectInput: Boolean): Resource[F, Process] =
      Resource.make(Sync[F].delay(processBuilder.run(connectInput)))(process => Sync[F].delay(process.destroy()))

    override def run(processBuilder: ProcessBuilder, log: ProcessLogger, connectInput: Boolean): Resource[F, Process] =
      Resource.make(Sync[F].delay(processBuilder.run(log, connectInput)))(process => Sync[F].delay(process.destroy()))
}
