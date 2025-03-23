import com.dimafeng.testcontainers.GenericContainer
import generated.ProjectBuildInfo

object MediaWorker {
  def apply(interconnectHost: String, interconnectPort: Int): GenericContainer = {
    GenericContainer(
      s"${ProjectBuildInfo.organization}/${ProjectBuildInfo.name}:${ProjectBuildInfo.version}",
      command = List(interconnectHost, interconnectPort.toString)
    )
  }
}
