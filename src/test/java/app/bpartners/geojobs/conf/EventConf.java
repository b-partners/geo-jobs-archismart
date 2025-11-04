package app.bpartners.geojobs.conf;

import app.bpartners.geojobs.PojaGenerated;
import org.springframework.test.context.DynamicPropertyRegistry;

@PojaGenerated
@SuppressWarnings("all")
public class EventConf {

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("aws.region", () -> "dummy-region");
    registry.add("aws.sqs.queue.url", () -> "dummy-queue-url");
    registry.add("aws.eventBridge.bus", () -> "dummy-bus-url");
  }
}
