package domain.client;

import domain.HlsSink;
import domain.RtmpSource;
import domain.command.RecordVideoSource;
import org.junit.jupiter.api.Test;

public class JavaPakMediaWorkerClientTest {

    @Test
    public void test() {
        JavaClient client = JavaClient.apply();


        client.executeCommand(
                new RecordVideoSource(
                        new RtmpSource("rtmpSource"),
                        new HlsSink("recordThis")
                )
        );

        System.out.println("test");
    }
}
