package medwork.server

import com.comcast.ip4s.Host
import org.scalatest.{flatspec, matchers}

class MainSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  "Host" should "accept docker container names" in {
    Host.fromString("cool-murdock").get
  }

}
