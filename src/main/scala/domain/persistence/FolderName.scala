package domain.persistence

import cats.effect.kernel.Clock
import domain.{HlsSink, Name}

type FolderName[A] = String

object FolderName {  
  given [F[_]: Clock](using sinkName: Name[HlsSink]): FolderName[HlsSink] =
    s"$sinkName${summon[Clock[F]].realTime.toString}"
}