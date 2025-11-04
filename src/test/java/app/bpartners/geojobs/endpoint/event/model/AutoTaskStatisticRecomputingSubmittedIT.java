package app.bpartners.geojobs.endpoint.event.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AutoTaskStatisticRecomputingSubmittedIT extends FacadeIT {

  @Autowired ObjectMapper om;

  @Test
  void serialize_then_deserialize() throws JsonProcessingException {
    var statisticRecomputingSubmitted = new AutoTaskStatisticRecomputingSubmitted("jobId");

    var serialized = om.writeValueAsString(statisticRecomputingSubmitted);
    var deserialized = om.readValue(serialized, AutoTaskStatisticRecomputingSubmitted.class);

    assertEquals(statisticRecomputingSubmitted, deserialized);
    assertEquals("jobId", deserialized.getJobId());
    assertEquals(Duration.ofSeconds(30), deserialized.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(60), deserialized.maxConsumerBackoffBetweenRetries());
  }
}
