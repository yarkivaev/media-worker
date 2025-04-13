package medwork.server.persistence

import cats.effect.kernel.Clock
import medwork.{HlsSink, Name}
import cats.effect.kernel.Sync

import cats.implicits._

/** Provides folder name, where object A should be stored
  * @tparam A
  *   stored object type
  */
type FolderName[F[_], A] = A => F[String]
