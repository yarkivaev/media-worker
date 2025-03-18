package domain.server.streaming

import cats.effect.IO
import cats.effect.kernel.Async
import cats.effect.unsafe.implicits.global
import org.scalatest.{flatspec, matchers}

import scala.concurrent.duration.*
import scala.sys.process.*

class RunProcessSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  "Process" should "be cancelable" in {
    val command =
      """
        |bash -c 'while true; do echo "Hello, world!"; sleep 1; done'
        |""".stripMargin
    var counter = 0
    var last_counter = 0
    val fiber =
      Async[IO]
        .start(
          RunProcess[IO]
            .run(
              command,
              ProcessLogger(line => {
                println(line)
                counter = counter + 1
              })
            )
            .useForever
        )
        .unsafeRunSync()

    (IO.sleep(2.second) *>
      fiber.cancel).unsafeRunSync()
    last_counter = counter
    IO.sleep(3.second).unsafeRunSync()
    assert(last_counter == counter)
  }

}
