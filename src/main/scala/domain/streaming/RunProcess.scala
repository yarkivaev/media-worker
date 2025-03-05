package domain.streaming

import cats.effect.kernel.{MonadCancel, Resource}
import cats.effect.{IO, Sync}

import scala.sys.process.{Process, ProcessBuilder, ProcessIO, ProcessLogger}

trait RunProcess[F[_]] {
  def run(processBuilder: ProcessBuilder): Resource[F, Process]

  def run(processBuilder: ProcessBuilder, log: ProcessLogger): Resource[F, Process]

  def run(processBuilder: ProcessBuilder, io: ProcessIO): Resource[F, Process]

  def run(processBuilder: ProcessBuilder, connectInput: Boolean): Resource[F, Process]

  def run(processBuilder: ProcessBuilder, log: ProcessLogger, connectInput: Boolean): Resource[F, Process]
}

object RunProcess {
  def apply[F[_] : Sync](using monadCancel: MonadCancel[F, Throwable]): RunProcess[F] = new RunProcess[F]:
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


  def fake: RunProcess[IO] = new RunProcess[IO] {
    private val fakeProcess: Process = new Process:
      override def isAlive(): Boolean = true

      override def exitValue(): Int = 0

      override def destroy(): Unit = ()

    override def run(processBuilder: ProcessBuilder): Resource[IO, Process] =
      Resource.eval(IO.println(processBuilder.toString)).map(_ => fakeProcess)

    override def run(processBuilder: ProcessBuilder, log: ProcessLogger): Resource[IO, Process] =
      Resource.eval(IO.println(processBuilder.toString)).map(_ => fakeProcess)

    override def run(processBuilder: ProcessBuilder, io: ProcessIO): Resource[IO, Process] =
      Resource.eval(IO.println(processBuilder.toString)).map(_ => fakeProcess)

    override def run(processBuilder: ProcessBuilder, connectInput: Boolean): Resource[IO, Process] =
      Resource.eval(IO.println(processBuilder.toString)).map(_ => fakeProcess)

    override def run(processBuilder: ProcessBuilder, log: ProcessLogger, connectInput: Boolean): Resource[IO, Process] =
      Resource.eval(IO.println(processBuilder.toString)).map(_ => fakeProcess)
  }
}