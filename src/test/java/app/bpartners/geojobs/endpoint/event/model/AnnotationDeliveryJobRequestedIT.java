package app.bpartners.geojobs.endpoint.event.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobRequested;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AnnotationDeliveryJobRequestedIT extends FacadeIT {
  @Autowired ObjectMapper om;

  private AnnotationDeliveryJobRequested annotationDeliveryJobRequested() {
    return AnnotationDeliveryJobRequested.builder()
        .jobId("jobId")
        .annotationJobWithObjectsIdFalsePositive("annotationJobFalsePositiveId")
        .annotationJobWithObjectsIdTruePositive("annotationJobTrueNegativeId")
        .annotationJobWithoutObjectsId("annotationJobWithoutObjectId")
        .build();
  }

  @Test
  void serialize_then_deserialize() throws JsonProcessingException {
    var serialized = om.writeValueAsString(annotationDeliveryJobRequested());
    var deserialized = om.readValue(serialized, AnnotationDeliveryJobRequested.class);

    assertEquals(annotationDeliveryJobRequested(), deserialized);
    assertEquals(Duration.ofMinutes(10), deserialized.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1), deserialized.maxConsumerBackoffBetweenRetries());
  }
}
