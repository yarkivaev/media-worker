package domain.persistence

import cats.effect.IO

trait Storage[F[_], A] {
  def save(obj: A): F[Unit]
}

object Storage {
  def fake[A]: Storage[IO, A] = (obj: A) => IO.pure(())
}
