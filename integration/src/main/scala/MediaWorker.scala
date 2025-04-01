import com.dimafeng.testcontainers.GenericContainer
import generated.ProjectBuildInfo

object MediaWorker {
  def apply(interconnectHost: String, interconnectPort: Int): GenericContainer = {
    GenericContainer(
      s"${ProjectBuildInfo.organization}/${ProjectBuildInfo.name}:${ProjectBuildInfo.version}",
      command = List(interconnectHost, interconnectPort.toString),
      env = Map.empty
        + ("QUEUE_HOST" -> "broker")
        + ("QUEUE_PORT" -> "5672")
        + ("S3_ENDPOINT_URL" -> "http://s3:9000/")
        + ("S3_ACCESS_KEY" -> "miniouser")
        + ("S3_SECRET_KEY" -> "miniopassword")
    )
  }
}
