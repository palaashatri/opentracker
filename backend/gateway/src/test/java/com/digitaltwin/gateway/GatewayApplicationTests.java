package com.digitaltwin.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"flights.processed", "ships.processed"})
class GatewayApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context starts successfully
        // with all beans wired (filters, controllers, SSE broadcaster, Kafka listeners)
    }
}
