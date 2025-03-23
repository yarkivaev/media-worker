import com.dimafeng.testcontainers.GenericContainer
import com.dimafeng.testcontainers.GenericContainer.FileSystemBind
import org.testcontainers.containers.BindMode

object DummyRtspStream {
  def apply(rtspServerHost: String, rtspServerPort: Int): GenericContainer =
    GenericContainer(
      "linuxserver/ffmpeg",
      command = Seq(
        "-re",
        "-i",
        "/BigBuckBunny_320x180.mp4",
        "-c:v",
        "libx264",
        "-preset",
        "veryfast",
        "-b:v",
        "1000k",
        "-c:a",
        "aac",
        "-b:a",
        "128k",
        "-f",
        "rtsp",
        s"rtsp://${rtspServerHost}:$rtspServerPort/test"
      ),
      fileSystemBind = Seq(
        FileSystemBind(
          getClass.getClassLoader.getResource("BigBuckBunny_320x180.mp4").getFile,
          "/BigBuckBunny_320x180.mp4",
          BindMode.READ_ONLY
        )
      )
    )
}
