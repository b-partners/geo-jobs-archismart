package app.bpartners.geojobs.repository;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.conf.FacadeIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AnnotationDeliveryConfigurationIT extends FacadeIT {
  @Autowired AnnotationDeliveryConfigurationRepository subject;

  @Test
  void read_default_latest_configuration() {
    var actual = subject.findLatestConfiguration();

    assertTrue(actual.isPresent());
    assertEquals(1.0, actual.get().getMinimumConfidenceForDelivery());
    assertNotNull(actual.get().getId());
    assertNotNull(actual.get().getCreationDatetime());
  }
}
