package domain.persistence

trait S3BucketStream[F[_]] {
  def act: F[Unit]
}
