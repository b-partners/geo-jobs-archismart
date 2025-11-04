package app.bpartners.geojobs.endpoint.event.model;

import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.model.tile.ParcelTilingTaskCreated;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ParcelTilingTaskCreatedIT extends FacadeIT {
  @Autowired ObjectMapper om;

  private ParcelTilingTaskCreated parcelTilingTaskCreated() {
    return new ParcelTilingTaskCreated(
        ParcelTilingTask.builder()
            .jobId("jobId")
            .parcels(List.of())
            .statusHistory(List.of())
            .submissionInstant(now())
            .build());
  }

  @Test
  void serialize_then_deserialize() throws JsonProcessingException {
    var serialized = om.writeValueAsString(parcelTilingTaskCreated());
    var deserialized = om.readValue(serialized, ParcelTilingTaskCreated.class);

    assertEquals(parcelTilingTaskCreated(), deserialized);
    assertEquals(Duration.ofSeconds(30), deserialized.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(60), deserialized.maxConsumerBackoffBetweenRetries());
  }
}
