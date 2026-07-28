package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.CreateDetectionDebugMode;
import app.bpartners.geojobs.endpoint.rest.model.DelimitationType;
import app.bpartners.geojobs.endpoint.rest.model.GeoJsonOutput;
import org.junit.jupiter.api.Test;

class CreateDetectionMapperTest {
  CreateDetectionMapper subject = new CreateDetectionMapper();

  @Test
  void from_null_debug_mode() {
    assertNull(subject.fromDebugMode(null));
  }

  @Test
  void from_debug_mode_ok() {
    var debugMode =
        new CreateDetectionDebugMode()
            .debugMode(true)
            .emailReceiver("email@receiver.com")
            .zoneName("zoneName")
            .geoJsonOutput(GeoJsonOutput.GEO_JSON)
            .needsImageOutput(true)
            .geoJsonDelimitationType(DelimitationType.ROOF)
            .toNotify(true);

    var actual = subject.fromDebugMode(debugMode);

    assertEquals(
        new CreateDetection()
            .emailReceiver("email@receiver.com")
            .zoneName("zoneName")
            .geoJsonOutput(GeoJsonOutput.GEO_JSON)
            .needsImageOutput(true)
            .geoJsonDelimitationType(DelimitationType.ROOF)
            .toNotify(true),
        actual);
  }
}
