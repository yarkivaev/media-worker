package domain

import org.scalatest.*
import org.scalatestplus.mockito.MockitoSugar
import cats.effect.*
import org.mockito.Mockito.*
import cats.effect.unsafe.implicits.global
import scala.collection.mutable
import org.mockito.ArgumentMatchers.*
import cats.effect.std.{Queue, Random}
import scala.concurrent.duration.DurationInt

class MediaWorkerRegistrySpec extends flatspec.AnyFlatSpec with MockitoSugar {

  // Mocks for dependencies
  val mockQueue: Queue[IO, MediaStream[IO]] = mock[Queue[IO, MediaStream[IO]]]
  implicit val mockRandom: Random[IO] = mock[Random[IO]]
  val mockMediaWorkerSup: (MediaWorkerId, MediaWorkerStatus, Int, Queue[IO, MediaStream[IO]]) => Resource[IO, MediaWorker[IO]] = mock[(MediaWorkerId, MediaWorkerStatus, Int, Queue[IO, MediaStream[IO]]) => Resource[IO, MediaWorker[IO]]]
  val mockStorage = mutable.Map.empty[MediaWorkerId, MediaWorker[IO]]

  // Test for starting a new worker
  "startNewWorker" should "generate a new MediaWorkerId and add the worker to storage" in {
    // Arrange
    val mockId = 123
    when(mockRandom.nextInt).thenReturn(IO.pure(mockId))
    var stoppedWorkers = 0
    val makeWorker = (iid: MediaWorkerId) => new MediaWorker[IO] {

      override val id: MediaWorkerId = iid
      override val status: MediaWorkerStatus = mock[MediaWorkerStatus]
      override val tasksCapacity: MediaStreamId = 1

      override def stop: IO[Unit] = IO.delay { stoppedWorkers = stoppedWorkers + 1 }
    }
    val storage = mutable.Map.empty[MediaWorkerId, MediaWorker[IO]]

    val registryResource: Resource[IO, MediaWorkerRegistry[IO]] = MediaWorkerRegistryImpl[IO](
      storage,
      (id, _, _, _) => Resource.eval { ??? },
      workerCapacity = 10,
      queue = mockQueue
    )

    registryResource.use { registry =>
      for {
        workerId <- registry.startNewWorker()
        _ <- IO.sleep(1.second)
        _ <- IO.delay {
          assert(workerId == mockId)
          assert(storage.contains(mockId))
        }
      } yield ()
    }.unsafeRunSync()

    assert(stoppedWorkers == 2)
  }

  // Test for stopping a media worker
  "stopMediaWorker" should "return Right if the worker exists" in {
    // Arrange
    val mockId = 123
    val mockWorker = mock[MediaWorker[IO]]
    when(mockStorage.get(mockId)).thenReturn(Some(mockWorker))
    when(mockWorker.stop).thenReturn(IO.unit)

    val registry = new MediaWorkerRegistryImpl[IO](
      mockStorage,
      mockMediaWorkerSup,
      workerCapacity = 10,
      queue = mockQueue
    )

    // Act
    val result = registry.stopMediaWorker(mockId)

    // Assert
    result.map {
      case Right(_) => succeed  // Ensure Right is returned for successful stop
      case Left(_) => fail("Expected Right, got Left")
    }
  }

  it should "return Left if the worker does not exist" in {
    // Arrange
    val mockId = 123
    when(mockStorage.get(mockId)).thenReturn(None)

    val registry = new MediaWorkerRegistryImpl[IO](
      mockStorage,
      mockMediaWorkerSup,
      workerCapacity = 10,
      queue = mockQueue
    )

    // Act
    val result = registry.stopMediaWorker(mockId)

    // Assert
    result.map {
      case Left(e) => assert(e.getMessage == "No such media worker")  // Ensure Left with the correct message
      case Right(_) => fail("Expected Left, got Right")
    }
  }

  // Test for stopping all workers
  "stopAllWorkers" should "stop all workers in the storage" in {
    // Arrange
    val mockId1: Int = 123
    val mockId2: Int = 456
    val mockWorker1 = mock[MediaWorker[IO]]
    val mockWorker2 = mock[MediaWorker[IO]]
    val storage = mutable.Map.empty[MediaWorkerId, MediaWorker[IO]] + (mockId1 -> mockWorker1, mockId2 -> mockWorker2)
    var stoppedWorkers = 0
    when(mockWorker1.stop).thenReturn(IO.delay { stoppedWorkers = stoppedWorkers + 1 } *> IO.unit)
    when(mockWorker2.stop).thenReturn(IO.delay { stoppedWorkers = stoppedWorkers + 1 } *> IO.unit)

    val registry = new MediaWorkerRegistryImpl[IO](
      storage,
      mockMediaWorkerSup,
      workerCapacity = 10,
      queue = mockQueue
    )

    // Act
    val result = registry.stopAllWorkers

    // Assert
    result.map(_ => succeed) // Ensure no errors are thrown
    assert(stoppedWorkers == 2)
  }
}
