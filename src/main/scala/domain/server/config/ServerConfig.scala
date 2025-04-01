package domain.server.config

import cats.effect.IO
import pureconfig.ConfigSource
import pureconfig.generic.derivation.default.*
import pureconfig.ConfigReader

final case class ServerConfig(
  queue: QueueConfig,
  s3: S3Config
) derives ConfigReader

object ServerConfig {
  def load(configSource: ConfigSource): IO[ServerConfig] =
    IO.delay(configSource.loadOrThrow[ServerConfig])
}

final case class QueueConfig(
  host: String,
  port: String
) derives ConfigReader

final case class S3Config(
  endpointUrl: String,
  accessKey: String,
  secretKey: String
) derives ConfigReader
