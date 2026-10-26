package app.bpartners.geojobs.service.coverage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.service.google.maps.GeoCodeApi;
import app.bpartners.geojobs.service.google.maps.GeoPosition;
import java.io.IOException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class CoverageLocationResolverTest {
  GeoCodeApi geoCodeApiMock = mock();
  CoverageLocationResolver subject = new CoverageLocationResolver(geoCodeApiMock);

  private void assertBadRequest(String address, Double longitude, Double latitude) {
    assertThrows(BadRequestException.class, () -> subject.resolve(address, longitude, latitude));
    verifyNoInteractions(geoCodeApiMock);
  }

  @Test
  void resolve_from_coordinates_ok() {
    assertEquals(new CoverageLocation(2.35, 48.85), subject.resolve(null, 2.35, 48.85));
    assertEquals(new CoverageLocation(-180, -90), subject.resolve("  ", -180.0, -90.0));
    assertEquals(new CoverageLocation(180, 90), subject.resolve(null, 180.0, 90.0));
  }

  @SneakyThrows
  @Test
  void resolve_from_address_ok() {
    when(geoCodeApiMock.searchGeoPositionFromAddress("paris"))
        .thenReturn(new GeoPosition(48.85, 2.35));

    assertEquals(new CoverageLocation(2.35, 48.85), subject.resolve("paris", null, null));
  }

  @Test
  void resolve_ko_with_both_address_and_coordinates() {
    assertBadRequest("paris", 2.35, 48.85);
    assertBadRequest("paris", 2.35, null);
  }

  @Test
  void resolve_ko_without_address_nor_coordinates() {
    assertBadRequest(null, null, null);
    assertBadRequest("  ", null, null);
  }

  @Test
  void resolve_ko_with_only_one_coordinate() {
    assertBadRequest(null, 2.35, null);
    assertBadRequest(null, null, 48.85);
  }

  @Test
  void resolve_ko_with_out_of_range_coordinates() {
    assertBadRequest(null, 181.0, 48.85);
    assertBadRequest(null, -181.0, 48.85);
    assertBadRequest(null, 2.35, 90.5);
    assertBadRequest(null, 2.35, -90.5);
  }

  @SneakyThrows
  @Test
  void resolve_bad_request_when_address_is_not_found() {
    when(geoCodeApiMock.searchGeoPositionFromAddress("nowhere"))
        .thenThrow(new IllegalStateException("No geocoding result"));

    assertThrows(BadRequestException.class, () -> subject.resolve("nowhere", null, null));
  }

  @SneakyThrows
  @Test
  void resolve_bad_request_when_geocoding_provider_fails() {
    when(geoCodeApiMock.searchGeoPositionFromAddress("paris")).thenThrow(new IOException("down"));

    assertThrows(BadRequestException.class, () -> subject.resolve("paris", null, null));
  }

  @SneakyThrows
  @Test
  void resolve_bad_request_and_keeps_interrupt_flag_when_interrupted() {
    when(geoCodeApiMock.searchGeoPositionFromAddress("paris"))
        .thenThrow(new InterruptedException("interrupted"));

    assertThrows(BadRequestException.class, () -> subject.resolve("paris", null, null));
    assertTrue(Thread.interrupted());
  }
}
