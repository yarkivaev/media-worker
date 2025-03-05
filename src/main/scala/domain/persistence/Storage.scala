package domain.persistence

import cats.effect.{IO, Sync}

import java.io.File

trait Storage[F[_], A] {
  def save(obj: A): F[Unit]
}

object Storage {
  def fake[A]: Storage[IO, A] = new Storage[IO, A] {
    override def save(obj: A): IO[Unit] = IO.pure(())
  }
}

class S3FileStorage[F[_] : Sync] extends Storage[F, File] {

  override def save(obj: File): F[Unit] = Sync[F].delay(()) // Save file to s3
}