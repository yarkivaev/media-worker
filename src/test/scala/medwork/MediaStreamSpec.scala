package medwork

import cats.effect.Async
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import medwork.server.persistence.FolderName
import medwork.server.persistence.Storage
import org.scalatest.flatspec
import org.scalatest.matchers
import os.Path

import scala.concurrent.duration._

class MediaStreamSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  "HlsSink" should "should save itself" in {
    var outputs = 0
    var segments = 0

    given Storage[IO, Path] = path =>
      IO {
        if path.last.startsWith("output") then outputs = outputs + 1
        else ()
        if path.last.startsWith("segment") then segments = segments + 1
        else ()
      }

    val folderName = "test_storage"
    if (os.exists(os.pwd / folderName)) {
      os.remove(os.pwd / folderName)
    }
    os.makeDir(os.pwd / folderName)

    given FolderName[IO, HlsSink] = _ => IO.pure(folderName)

    val fiber = Async[IO].start(summon[Storage[IO, HlsSink]].save(HlsSink("test_storage"))).unsafeRunSync()

    os.write(os.pwd / folderName / "output.m3u8", "hello world")
    os.write(os.pwd / folderName / "segment_001.ts", "hello world")

    IO.sleep(5.second).unsafeRunSync()

    fiber.cancel.unsafeRunSync()
    assert(!os.exists(os.pwd / folderName / "segment_001.ts"))
    assert(outputs > 0)
    assert(segments == 1)

    os.remove.all(os.pwd / folderName)

  }
}
