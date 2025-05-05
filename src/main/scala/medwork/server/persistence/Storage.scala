package medwork.server.persistence

import cats.effect.IO

/** Represents external file storage
  * @tparam F
  *   Effect
  * @tparam A
  *   Stored content
  */
trait Storage[F[_], A] {
  def save(obj: A): F[Unit]
}

object Storage {
  def fake[A]: Storage[IO, A] = new Storage[IO, A] {
    override def save(obj: A): IO[Unit] = IO.pure(())
  }
}
