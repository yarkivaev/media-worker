package domain.persistence

import cats.effect.{Concurrent, IO}

trait Storage[F[_]: Concurrent, A] {
  def save(obj: A): F[Unit]
}

object Storage {
  def fake[A]: Storage[IO, A] = new Storage[IO, A] {
    override def save(obj: A): IO[Unit] = IO.pure(())
  }
}
