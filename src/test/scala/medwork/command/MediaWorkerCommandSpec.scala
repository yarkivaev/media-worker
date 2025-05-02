package medwork.command

import cats.effect.unsafe.implicits.global
import cats.effect.{Async, IO}
import medwork.server.persistence.{FolderName, Storage}
import org.scalatest.{flatspec, matchers}
import os.Path
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import medwork.*
import medwork.command.MediaWorkerCommand

import scala.concurrent.duration.*

class MediaStreamSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  "Decoder" should "decode SaveStream" in {
    import medwork.command.MediaWorkerCommand

    val command: MediaWorkerCommand = SaveStream(
            RtmpSource("url"),
            HlsSink("140")
        )
    val raw: Json = Encoder[MediaWorkerCommand]()(
        command
    )
    assert(Decoder[MediaWorkerCommand].decodeJson(raw).right.get == command)
  }

  "Decoder" should "decode RedirectStream" in {
    import medwork.command.MediaWorkerCommand

    val command: MediaWorkerCommand = RedirectStream(
            RtmpSource("url"),
            HlsSink("140")
        )
    val raw: Json = Encoder[MediaWorkerCommand]()(
        command
    )
    assert(Decoder[MediaWorkerCommand].decodeJson(raw).right.get == command)
  }
}
