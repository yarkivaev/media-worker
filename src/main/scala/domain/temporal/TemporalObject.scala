package domain.temporal

import cats.effect.IO

trait TemporalObject[F[_], A] {
  def path(obj: A): String

  def save: F[Unit]
}

class TemporalObjectImpl[A] extends TemporalObject[IO, A] {

  override def path(obj: A): String = "output.m3u8"

  override def save: IO[Unit] = IO.println("saved!")
}