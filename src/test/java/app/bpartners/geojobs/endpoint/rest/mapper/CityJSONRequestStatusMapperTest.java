package app.bpartners.geojobs.endpoint.rest.mapper;

import static app.bpartners.geojobs.endpoint.rest.model.CityJSONRequestStatus.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.cityjson.CityJSONRequestStatusMapper;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus;
import org.junit.jupiter.api.Test;

class CityJSONRequestStatusMapperTest {
  @Test
  void null_to_rest() {
    assertNull(CityJSONRequestStatusMapper.toRest(null));
  }

  @Test
  void finished_to_rest() {
    assertEquals(FINISHED, CityJSONRequestStatusMapper.toRest(CityJSONRequestStatus.FINISHED));
  }

  @Test
  void unavailable_to_rest() {
    assertEquals(
        UNAVAILABLE, CityJSONRequestStatusMapper.toRest(CityJSONRequestStatus.UNAVAILABLE));
  }

  @Test
  void failed_to_rest() {
    assertEquals(FAILED, CityJSONRequestStatusMapper.toRest(CityJSONRequestStatus.FAILED));
  }

  @Test
  void processing_to_rest() {
    assertEquals(PROCESSING, CityJSONRequestStatusMapper.toRest(CityJSONRequestStatus.PROCESSING));
  }

  @Test
  void failed_to_generic_status_exposes_message() {
    var status =
        CityJSONRequestStatusMapper.toGenericStatusRest(
            CityJSONRequestStatus.FAILED, "Invalid CityJSON file");

    assertEquals(Status.HealthEnum.FAILED, status.getHealth());
    assertEquals("Invalid CityJSON file", status.getMessage());
  }

  @Test
  void finished_to_generic_status_has_no_message() {
    var status =
        CityJSONRequestStatusMapper.toGenericStatusRest(CityJSONRequestStatus.FINISHED, null);

    assertEquals(Status.HealthEnum.SUCCEEDED, status.getHealth());
    assertNull(status.getMessage());
  }
}
