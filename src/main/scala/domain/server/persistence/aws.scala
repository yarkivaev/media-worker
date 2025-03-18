package domain.server.persistence

import cats.effect.Sync
import io.minio.{MinioClient, UploadObjectArgs}
import os.Path

object aws {
  given [F[_]: Sync](using minioClient: MinioClient): Storage[F, Path] = path =>
    Sync[F].delay({
      val workDir = os.pwd
      val relativePath = path.relativeTo(workDir)
      val bucketName = relativePath.segments.head
      val key = relativePath.segments.tail.toList.mkString("_")
      val request = UploadObjectArgs
        .builder()
        .bucket(bucketName)
        .`object`(key)
        .filename(path.toString)
        .build()
      minioClient.uploadObject(request)
    })
}
