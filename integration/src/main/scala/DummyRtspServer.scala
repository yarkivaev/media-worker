import com.dimafeng.testcontainers.GenericContainer

object DummyRtspServer {
  def apply(): GenericContainer =
    GenericContainer(
      "bluenviron/mediamtx:latest",
      exposedPorts = Seq(8554, 1935, 8888)
    )
}
