package app.bpartners.geojobs.endpoint.event.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.model.status.ParcelDetectionStatusRecomputingSubmitted;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ParcelDetectionStatusRecomputingSubmittedIT extends FacadeIT {
  @Autowired ObjectMapper om;

  @Test
  void serialize_then_deserialize() throws JsonProcessingException {
    var object = new ParcelDetectionStatusRecomputingSubmitted("id");

    var serialized = om.writeValueAsString(object);
    var deserialized = om.readValue(serialized, ParcelDetectionStatusRecomputingSubmitted.class);

    assertEquals(object, deserialized);
  }
}
