package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.service.geojson.GeometryConverter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class GeometryConverterTest {
  GeometryConverter subject = new GeometryConverter();

  @Test
  void retrieve_geometry_from_tile_coordinates() {
    var actual = subject.getMultiPolygonFromTile(544680, 383095, 20);

    var actualGeometryAsString = subject.writeGeometryAsString(actual);
    assertEquals(expectedGeometryFromTileCoordinates(), actualGeometryAsString);
  }

  private String expectedGeometryFromTileCoordinates() {
    return "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[7.00103759765625,43.55053877556738],[7.00103759765625,43.55078760402636],[7.001380920410156,43.55078760402636],[7.001380920410156,43.55053877556738],[7.00103759765625,43.55053877556738]]]]}";
  }
}
