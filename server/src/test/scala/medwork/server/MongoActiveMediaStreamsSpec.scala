package medwork.server

import cats.effect._
import cats.effect.unsafe.implicits.global
import io.circe.generic.auto._
import medwork._
import medwork.server.persistence.Storage
import medwork.server.streaming.StreamingBackendImpl
import mongo4cats.circe._
import mongo4cats.client.MongoClient
import org.scalatest._
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

import scala.concurrent.duration.DurationInt

class MongoActiveMediaStreamsSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers with BeforeAndAfterAll {

  // Set up a MongoDB container
  val mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.4"))
  var resource: Resource[IO, MongoActiveMediaStreams[IO]] = _

  override def beforeAll(): Unit = {
    // Start MongoDB container
    mongoDBContainer.start()

    val mongoUri = mongoDBContainer.getReplicaSetUrl
    given Storage[IO, MediaSink] = Storage.fake[MediaSink]
    given StreamingBackendImpl[IO] = StreamingBackendImpl[IO]()

    println(mongoUri)

    resource = for {
      client <- MongoClient.fromConnectionString[IO](mongoUri)
      database <- Resource.eval(client.getDatabase("my-db"))
      collection <- Resource.eval(database.getCollectionWithCodec[MediaStream]("streams"))
    } yield new MongoActiveMediaStreams[IO](collection, ActiveMediaStreams.inMemory[IO])
  }

  override def afterAll(): Unit = {
    // Stop MongoDB container after tests
    mongoDBContainer.stop()
  }

  it should "stop a media stream" in {
    val mediaStream = MediaStream(RtmpSource("source"), RtmpSink("sink"))

    resource
      .use(mongoActiveMediaStreams => {
        // Ensure the stream is initially managed and then stop it
        val fiber = Async[IO].start(mongoActiveMediaStreams.manageMediaStream(mediaStream)).unsafeRunSync()
        IO.sleep(2.second).unsafeRunSync()
        val resultBeforeStop = mongoActiveMediaStreams.contains(mediaStream).unsafeRunSync()

        // IO.never.unsafeRunSync()
        resultBeforeStop should be(true)

        println(resultBeforeStop)

        // Stop the stream
        mongoActiveMediaStreams.stopMediaStream(mediaStream).unsafeRunSync()

        // Verify the stream has been removed
        val resultAfterStop = mongoActiveMediaStreams.contains(mediaStream).unsafeRunSync()
        resultAfterStop should be(false)
        IO.unit
      })
      .unsafeRunSync()
  }
}
