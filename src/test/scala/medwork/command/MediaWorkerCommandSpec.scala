package medwork.command
import io.circe._
import medwork._
import org.scalatest.flatspec
import org.scalatest.matchers

class MediaStreamSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  "Decoder" should "decode StartMediaStream" in {

    val command: MediaWorkerCommand = StartMediaStream(
      RtmpSource("url"),
      HlsSink("140")
    )
    val raw: Json = Encoder[MediaWorkerCommand]()(
      command
    )
    assert(Decoder[MediaWorkerCommand].decodeJson(raw).right.get === command)
  }

  "Decoder" should "decode StopMediaStream" in {

    val command: MediaWorkerCommand = StopMediaStream(
      RtmpSource("url"),
      HlsSink("140")
    )
    val raw: Json = Encoder[MediaWorkerCommand]()(
      command
    )
    assert(Decoder[MediaWorkerCommand].decodeJson(raw).right.get === command)
  }
}
