import com.dimafeng.testcontainers.GenericContainer
import generated.ProjectBuildInfo

object MediaWorker {
  def apply(interconnectHost: String, s3Host: String, interconnectPort: Int): GenericContainer = {
    GenericContainer(
      s"${ProjectBuildInfo.organization}/${ProjectBuildInfo.name}:${ProjectBuildInfo.version}",
      command = List(interconnectHost, interconnectPort.toString),
      env = Map.empty
        + ("QUEUE_HOST" -> interconnectHost)
        + ("QUEUE_PORT" -> "5672")
        + ("S3_ENDPOINT_URL" -> f"http://$s3Host:9000/")
        + ("S3_ACCESS_KEY" -> "miniouser")
        + ("S3_SECRET_KEY" -> "miniopassword")
    )
  }
}
