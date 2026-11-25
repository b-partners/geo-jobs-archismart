package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.google.geocoding.api.GeoJsonFeature;
import app.bpartners.geojobs.service.google.geocoding.api.GoogleGeocodingClientApi;
import app.bpartners.geojobs.service.google.geocoding.api.exception.GeocodingException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DetectionAddressResolverTest {
  GoogleGeocodingClientApi geocodingClientApiMock = mock();
  DetectionAddressResolver subject = new DetectionAddressResolver(geocodingClientApiMock);

  @Test
  void resolve_address_from_coordinates_zone_name() {
    when(geocodingClientApiMock.findBuildingByLocation(43.5577692, 7.0072513))
        .thenReturn(Optional.of(buildingWith(Map.of("formattedAddress", "1 rue de Cannes"))));

    assertEquals("1 rue de Cannes", subject.apply(detectionNamed("43.5577692, 7.0072513")));
  }

  @Test
  void resolve_address_from_signed_coordinates_without_spaces() {
    when(geocodingClientApiMock.findBuildingByLocation(-33.8688, 151.2093))
        .thenReturn(Optional.of(buildingWith(Map.of("formattedAddress", "Sydney"))));

    assertEquals("Sydney", subject.apply(detectionNamed("  -33.8688,+151.2093 ")));
  }

  @Test
  void resolve_address_from_integer_coordinates() {
    when(geocodingClientApiMock.findBuildingByLocation(43.0, 7.0))
        .thenReturn(Optional.of(buildingWith(Map.of("formattedAddress", "Somewhere"))));

    assertEquals("Somewhere", subject.apply(detectionNamed("43,7")));
  }

  @Test
  void fall_back_on_zone_name_when_geocoding_finds_nothing() {
    when(geocodingClientApiMock.findBuildingByLocation(anyDouble(), anyDouble()))
        .thenReturn(Optional.empty());

    assertEquals("43.5, 7.0", subject.apply(detectionNamed("43.5, 7.0")));
  }

  @Test
  void fall_back_on_zone_name_when_geocoded_address_is_missing() {
    when(geocodingClientApiMock.findBuildingByLocation(anyDouble(), anyDouble()))
        .thenReturn(Optional.of(buildingWith(Map.of("placeId", "place"))));

    assertEquals("43.5, 7.0", subject.apply(detectionNamed("43.5, 7.0")));
  }

  @Test
  void fall_back_on_zone_name_when_geocoded_address_is_not_a_string() {
    when(geocodingClientApiMock.findBuildingByLocation(anyDouble(), anyDouble()))
        .thenReturn(Optional.of(buildingWith(Map.of("formattedAddress", 42))));

    assertEquals("43.5, 7.0", subject.apply(detectionNamed("43.5, 7.0")));
  }

  @Test
  void fall_back_on_zone_name_when_geocoded_address_is_blank() {
    when(geocodingClientApiMock.findBuildingByLocation(anyDouble(), anyDouble()))
        .thenReturn(Optional.of(buildingWith(Map.of("formattedAddress", " "))));

    assertEquals("43.5, 7.0", subject.apply(detectionNamed("43.5, 7.0")));
  }

  @Test
  void fall_back_on_zone_name_when_geocoding_fails() {
    when(geocodingClientApiMock.findBuildingByLocation(anyDouble(), anyDouble()))
        .thenThrow(new GeocodingException("boom", new RuntimeException()));

    assertEquals("43.5, 7.0", subject.apply(detectionNamed("43.5, 7.0")));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "detection zone name",
        "43.5",
        "43.5; 7.0",
        "43.5, 7.0, 3.0",
        "lat, lon",
        "91.0, 7.0",
        "-90.5, 7.0",
        "43.5, 181.0",
        "43.5, -180.5"
      })
  void do_not_geocode_zone_names_that_are_not_coordinates(String zoneName) {
    assertEquals(zoneName, subject.apply(detectionNamed(zoneName)));

    verifyNoInteractions(geocodingClientApiMock);
  }

  @Test
  void do_not_geocode_without_zone_name() {
    assertNull(subject.apply(detectionNamed(null)));

    verifyNoInteractions(geocodingClientApiMock);
  }

  private static Detection detectionNamed(String zoneName) {
    var detection = mock(Detection.class);
    when(detection.getZoneName()).thenReturn(zoneName);
    return detection;
  }

  private static GeoJsonFeature buildingWith(Map<String, Object> properties) {
    return GeoJsonFeature.of(Map.of(), properties);
  }
}
