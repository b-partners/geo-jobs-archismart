package app.bpartners.geojobs.endpoint.event.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.List;
import org.junit.jupiter.api.Test;

class ZoneTilingJobStatusChangedTest {

  @Test
  void testToString() {
    var statusChanged = new ZoneTilingJobStatusChanged();
    statusChanged.setOldJob(ZoneTilingJob.builder().id("oldId").build());
    statusChanged.setNewJob(ZoneTilingJob.builder().id("newId").build());

    List<Object> eventsAsObjects = List.of(statusChanged);
    var asString = eventsAsObjects.toString();

    assertTrue(asString.contains("oldId"));
    assertTrue(asString.contains("newId"));
  }
}
