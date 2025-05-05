package medwork.server

import cats.effect._
import cats.effect.std.Queue
import cats.effect.std.Random
import cats.effect.unsafe.implicits.global
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest._
import org.scalatestplus.mockito.MockitoSugar

import scala.collection.mutable
import scala.concurrent.duration.DurationInt

//class MediaWorkerRegistrySpec extends flatspec.AnyFlatSpec with MockitoSugar {
//
//  val mockQueue: Queue[IO, MediaStream[IO]] = mock[Queue[IO, MediaStream[IO]]]
//  implicit val mockRandom: Random[IO] = mock[Random[IO]]
//  val mockMediaWorkerSup: (MediaWorkerId, MediaWorkerStatus, Int, Queue[IO, MediaStream[IO]]) => Resource[IO, MediaWorker[IO]] = mock[(MediaWorkerId, MediaWorkerStatus, Int, Queue[IO, MediaStream[IO]]) => Resource[IO, MediaWorker[IO]]]
//
//  "startNewWorker" should "generate a new MediaWorkerId and add the worker to storage" in {
//    val mockId = 123
//    when(mockRandom.nextInt).thenReturn(IO.pure(mockId))
//    var stoppedWorkers = 0
//    val makeWorker = (iid: MediaWorkerId) => new MediaWorker[IO] {
//
//      override val id: MediaWorkerId = iid
//      override val status: MediaWorkerStatus = mock[MediaWorkerStatus]
//      override val tasksCapacity: MediaStreamId = 1
//
//      override def stop: IO[Unit] = IO.delay { stoppedWorkers = stoppedWorkers + 1 }
//    }
//    val storage = mutable.Map.empty[MediaWorkerId, MediaWorker[IO]]
//
//    val registryResource: Resource[IO, MediaWorkerRegistry[IO]] = MediaWorkerRegistryImpl[IO](
//      storage,
//      (id, _, _, _) => Resource.eval(IO(makeWorker(id))),
//      workerCapacity = 10,
//      queue = mockQueue
//    )
//
//    registryResource.use({ registry =>
//      for {
//        workerId <- registry.startNewWorker()
//        _ <- IO.sleep(1.second)
//        _ <- IO.delay {
//          assert(workerId == mockId)
//          assert(storage.contains(mockId))
//        }
//      } yield ()
//    }).unsafeRunSync()
//
//    assert(stoppedWorkers == 1)
//  }
//
//  "stopMediaWorker" should "return Right if the worker exists" in {
//    val mockId = 123
//    val mockWorker = mock[MediaWorker[IO]]
//    val storage = mutable.Map(mockId -> mockWorker)
//    var stopped = 0
//    when(mockWorker.stop).thenReturn(IO.delay { stopped += + 1} *> IO.unit)
//
//    val registryResource = MediaWorkerRegistryImpl[IO](
//      storage,
//      mockMediaWorkerSup,
//      workerCapacity = 10,
//      queue = mockQueue
//    )
//
//    registryResource.use { registry =>
//      for {
//        result <- registry.stopMediaWorker(mockId)
//      } yield result match {
//        case Right(_) => succeed  // Ensure Right is returned for successful stop
//        case Left(_) => fail("Expected Right, got Left")
//      }
//    }.unsafeRunSync()
//
//    assert(stopped == 1)
//  }
//
//  it should "return Left if the worker does not exist" in {
//    val mockId = 123
//    val storage = mutable.Map.empty[MediaWorkerId, MediaWorker[IO]]
//
//    val registryResource = MediaWorkerRegistryImpl[IO](
//      storage,
//      mockMediaWorkerSup,
//      workerCapacity = 10,
//      queue = mockQueue
//    )
//
//    registryResource.use { registry =>
//      for {
//        result <- registry.stopMediaWorker(mockId)
//      } yield result match {
//        case Left(e) => assert(e.getMessage == "No such media worker")  // Ensure Left with the correct message
//        case Right(_) => fail("Expected Left, got Right")
//      }
//    }.unsafeRunSync()
//  }
//}
