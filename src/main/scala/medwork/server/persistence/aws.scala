package medwork.server.persistence

import cats.effect.Sync
import io.minio.{MinioClient, UploadObjectArgs}
import os.Path
import scala.util.Try
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import cats.effect.std.Semaphore
import cats.implicits._
import cats.syntax._

object aws {
  given [F[_]: Sync](using bucketSemaphore: String => F[Semaphore[F]], minioClient: MinioClient): Storage[F, Path] =
    path =>
      for {
        workDir <- Sync[F].delay(os.pwd)
        relativePath = path.relativeTo(workDir)
        bucketName = relativePath.segments.head.toLowerCase.replace("_", "-")
        key = relativePath.segments.tail.toList.mkString("_")
        sem <- bucketSemaphore(bucketName)
        _ <- sem.permit.use(_ =>
          Sync[F].delay {
            val bucketExists = Try(
              minioClient.bucketExists(
                BucketExistsArgs
                  .builder()
                  .bucket(bucketName)
                  .build()
              )
            ).getOrElse(false)
            if (!bucketExists) {
              minioClient.makeBucket(
                MakeBucketArgs
                  .builder()
                  .bucket(bucketName)
                  .build()
              )
            }
            val request = UploadObjectArgs
              .builder()
              .bucket(bucketName)
              .`object`(key)
              .filename(path.toString)
              .build()
            val response = minioClient.uploadObject(request)
          }
        )
      } yield ()
}
