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

  "Decoder" should "decode StartMediaStream" in {
    import medwork.command.MediaWorkerCommand

    val command: MediaWorkerCommand = StartMediaStream(
            RtmpSource("url"),
            HlsSink("140")
        )
    val raw: Json = Encoder[MediaWorkerCommand]()(
        command
    )
    assert(Decoder[MediaWorkerCommand].decodeJson(raw).right.get == command)
  }

  "Decoder" should "decode StopMediaStream" in {
    import medwork.command.MediaWorkerCommand

    val command: MediaWorkerCommand = StopMediaStream(
            RtmpSource("url"),
            HlsSink("140")
        )
    val raw: Json = Encoder[MediaWorkerCommand]()(
        command
    )
    assert(Decoder[MediaWorkerCommand].decodeJson(raw).right.get == command)
  }
}
