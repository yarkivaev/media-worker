package domain.server.persistence

import cats.effect.kernel.Clock
import domain.{HlsSink, Name}

type FolderName[A] = A => String

object FolderName {
  given [F[_]: Clock](using sinkName: Name[HlsSink]): FolderName[HlsSink] =
    hlsSink => s"${sinkName(hlsSink)}${summon[Clock[F]].realTime.toString}"
}
