package app.bpartners.geojobs.model.geometry;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.endpoint.rest.postprocessing.GeoJsonLoader;
import app.bpartners.geojobs.service.GeometryPixelProjector;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.TileCoordinatesPolygonIntersection;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class VGGFactoryTest {
  private final GeoJsonLoader geoJsonLoader = new GeoJsonLoader();
  private final PolygonProvider polygonProvider =
      new PolygonProvider("/geometry/vgg/pathway.json", null, new IntXY(1024, 1024));
  GeometryConverter geometryConverter = new GeometryConverter(null, null);
  TileCoordinatesPolygonIntersection tileCoordinatesPolygonIntersection =
      new TileCoordinatesPolygonIntersection(new GeometryPixelProjector(), geometryConverter);
  GeometrySquareMeterArea geometrySquareMeterArea = new GeometrySquareMeterArea();
  ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  private final VGGFactory subject =
      new VGGFactory(
          tileCoordinatesPolygonIntersection,
          geometryConverter,
          geometrySquareMeterArea,
          objectMapper);

  @Test
  void features_to_vgg_ok() {
    var features = polygonProvider.getPolygons();
    var expectedFilename = "5cm3346073745629231615_20_538860_367572.jpg";

    var actual = subject.convert(features);

    assertEquals(5, actual.size());
    assertEquals(2, actual.get(expectedFilename).getRegions().size());
  }
}
