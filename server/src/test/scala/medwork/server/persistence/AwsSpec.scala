package medwork.server.persistence

import cats.effect.IO
import cats.effect.std.Semaphore
import cats.effect.unsafe.implicits.global
import io.minio.GetObjectArgs
import io.minio.ListObjectsArgs
import io.minio.MinioClient
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec
import org.scalatest.matchers
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import os.Path

import scala.io.Source

class AwsSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers with BeforeAndAfterAll {

  val container: GenericContainer[Nothing] = new GenericContainer(DockerImageName.parse("minio/minio"))
    .withExposedPorts(9000, 9001)
    .asInstanceOf[GenericContainer[Nothing]]
    .withCommand("server", "/data", "--console-address", ":9001")

  override def beforeAll(): Unit = {
    container.start()
    container.setPortBindings(java.util.List.of("9000", "9001"))
  }

  override def afterAll(): Unit = {
    container.stop()
  }

  "aws" should "store a file in MinIO" in {
    given minioClient: MinioClient = MinioClient
      .builder()
      .endpoint(s"http://${container.getHost}:${container.getMappedPort(9000)}")
      .credentials("minioadmin", "minioadmin")
      .build

    val bucketName = "hello"
    val content = "HelloWorld"
    val folderPath = os.pwd / bucketName

    if (os.exists(folderPath)) {
      os.remove.all(folderPath)
    }
    val dummyPath: Path = folderPath / "test-file.txt"
    os.makeDir(folderPath)
    os.write(dummyPath, content)

    // minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())

    val bucketSem: (String => IO[Semaphore[IO]]) = _ => Semaphore[IO](1)

    val storage = aws(bucketSem, minioClient)

    // Use the storage and check for file upload
    val result: Unit = storage.save(dummyPath).unsafeRunSync()

    val fileName = minioClient
      .listObjects(ListObjectsArgs.builder().bucket(bucketName).build())
      .iterator()
      .next()
      .get()
      .objectName()

    val source = Source.fromInputStream(
      minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).`object`(fileName).build())
    )

    os.remove.all(folderPath)

    assert(source.mkString === content)
  }
}
