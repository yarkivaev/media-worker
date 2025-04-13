package medwork.client;

import medwork.HlsSink;
import medwork.RtmpSource;
import medwork.command.SaveStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.RabbitMQContainer;

public class JavaClientTest {


    private static final RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3-management")
            .withExposedPorts(5672, 15672); // 5672 for AMQP, 15672 for Web UI

    @BeforeAll
    static void beforeAll() {
        rabbitMQ.start();
    }

    @AfterAll
    static void afterAll() {
        rabbitMQ.stop();
    }

    @Test
    public void test() {
        JavaClient client = JavaClient.apply(rabbitMQ.getAmqpPort());


        client.executeCommand(
                new SaveStream(
                        new RtmpSource("rtmpSource"),
                        new HlsSink("recordThis")
                )
        );
    }
}
