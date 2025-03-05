import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.comcast.ip4s.Port
import domain.*
import domain.server.MediaWorker
import domain.server.streaming.{FFMpegStreamingBackend, RunProcess, StreamingBackend, StreamingResource}
import lepus.client.LepusClient
import lepus.protocol.domains.QueueName

import scala.language.postfixOps
import scala.runtime.stdLibPatches.Predef.summon

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    given Name[HlsSink] = hlsSink => hlsSink.sinkName

    given RunProcess[IO] = RunProcess()

    given StreamingResource[MediaSource] = summon[StreamingResource[MediaSource]]

    given StreamingResource[MediaSink] = summon[StreamingResource[MediaSink]]

    given StreamingBackend[IO] = FFMpegStreamingBackend[IO]
    (for {
      lepusClient <- LepusClient[IO](port = Port.fromInt(5672).get)
      mediaWorker <- Resource.eval(
        MediaWorker(
          Broker.messageSource[IO, MediaWorkerCommand[IO]](
            lepusClient,
            QueueName("mediaworkercommand")
          )
        )
      )
    } yield ()).use_.as(ExitCode.Success)
  }
  //    mainThread(args)

  def tryCommand(args: List[String]): IO[ExitCode] =
    IO(os.proc("ls", "src").spawn()).map(res => print(res.stdout)).as(ExitCode.Success)

  //  def rabbitTry(args: List[String]): IO[ExitCode] =


  //  def mainThread(args: List[String]): IO[ExitCode] =
  //    (for {
  //      random <- Resource.eval(Random.scalaUtilRandom[IO])
  //      queue <- Resource.eval(Queue.unbounded[IO, MediaStream[IO]]())
  //      registry <- MediaWorkerRegistryImpl[IO](
  //        mutable.Map[MediaWorkerId, MediaWorker[IO]](),
  //        MediaWorkerImpl[IO](_, _, _, _),
  //        Integer.MAX_VALUE,
  //        queue
  //      )(implicitly[Monad[IO]], random, implicitly[Async[IO]])
  //      streamingBackend = StreamingBackendImpl[IO]
  //      mediaStream1 = MediaStreamImpl[IO](
  //        42,
  //        DateTime.now(),
  //        DateTime.now(),
  //        RtmpSource("helo"),
  //        HlsSink("helo"),
  //      )(streamingBackend)
  //      mediaStream2 = MediaStreamImpl[IO](
  //        42,
  //        DateTime.now(),
  //        DateTime.now(),
  //        RtspSource("helo"),
  //        RtmpSink("helo"),
  //      )(streamingBackend)
  //      id <- Resource.eval(registry.startNewWorker())
  ////      id2 <- Resource.eval(registry.startNewWorker())
  //      _ <- Resource.eval { queue.offer(mediaStream1) }
  //      _ <- Resource.eval{ Async[IO].start {
  //        def loop: IO[Unit] = {
  //          for {
  //            _ <- queue.offer(mediaStream1)
  //            _ <- Async[IO].sleep(2.second)
  //            _ <- loop
  //          } yield ()
  //        }
  //        loop
  //      } }
  //      _ <- Resource.eval{ Async[IO].start {
  //        def loop: IO[Unit] = {
  //          for {
  //            _ <- queue.offer(mediaStream2)
  //            _ <- Async[IO].sleep(1.5.second)
  //            _ <- loop
  //          } yield ()
  //        }
  //        loop
  //      } }
  //      _ <- Resource.eval(Async[IO].sleep(5.second))
  //      _ <- Resource.eval(registry.stopMediaWorker(id))
  //      a <- Resource.eval(IO.never.as(ExitCode.Success))
  //    } yield a).use_.as(ExitCode.Success) //ExitCode.Success
  //    BlazeServerBuilder[IO]
  //      .bindHttp(8080, "0.0.0.0")
  //      .withHttpApp(helloWorldService)
  //      .resource
  //      .use(_ => IO.never)
  //      .as(ExitCode.Success)
}
/*
Pak media worker:
- Serve media streams inside the hospital (pak)
- use ffmpeg, gstreamer, etg as backend
- have access to storage that keeps all running media processes
- can be controlled via Rest API, maintain responsibility to keep consistency
 -


Entities:
- Media stream
  - id
  - FFMpeg or GStreamer
  - FFMPEG: where-from, where-from-options, where-to, where-to-options
  - startDateTime
  - stopDateTime
  - Goal
    - save recording from video source
    - stream video source to local web rtc server
    - stream video source to global web rtc server
  - status: active, stopped, finished
- Media worker (adds itself to db, maintain active status. When worker is down, status became stopped)
  - id
  - serving media streams
  - status: active, stopped
  - tasks capacity


- Also we need a queue for workers to take tasks

Worker lifecycle:
- Boot, add itself to table of active workers
- Subscribe the queue, take tasks, execute them
 */
