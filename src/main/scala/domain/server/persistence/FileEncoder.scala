package domain.server.persistence

import java.io.File

trait FileEncoder[A] {
  def encode(obj: A): File
}
