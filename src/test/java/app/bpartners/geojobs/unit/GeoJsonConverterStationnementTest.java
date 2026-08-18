package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PARKING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.endpoint.rest.model.TileInfoSize;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.postprocessing.DetectionBoundaryMerger;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.geojson.GeoJsonConverter;
import app.bpartners.geojobs.service.geojson.GeoJsonMapper;
import app.bpartners.geojobs.service.geojson.GeoJsonMultiPolygonCorrector;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeoJsonConverterStationnementTest {
  private static final int TILE_X = 539081;
  private static final int TILE_Y = 367698;
  private static final int ZOOM = 20;

  private final GeometryConverter geometryConverter = new GeometryConverter();
  private final GeoJsonConverter subject =
      new GeoJsonConverter(
          new GeoJsonMapper(new GeoJsonMultiPolygonCorrector()),
          new DetectionBoundaryMerger(),
          geometryConverter);

  @Test
  void keeps_parking_detected_inside_provided_geometry() {
    var providedGeometry = geometryConverter.getMultiPolygonFromTile(TILE_X, TILE_Y, ZOOM);

    var actual = subject.apply(List.of(parkingDetectedTile()), providedGeometry);

    assertEquals(1, actual.getGeoFeatures().size());
    assertEquals(PARKING.name(), actual.getGeoFeatures().getFirst().getProperties().get("label"));
  }

  @Test
  void discards_parking_detected_outside_provided_geometry() {
    var providedGeometry = geometryConverter.getMultiPolygonFromTile(TILE_X + 10, TILE_Y, ZOOM);

    var actual = subject.apply(List.of(parkingDetectedTile()), providedGeometry);

    assertTrue(actual.getGeoFeatures().isEmpty());
  }

  private static DetectedTile parkingDetectedTile() {
    return DetectedTile.builder()
        .tile(
            Tile.builder()
                .size(new TileInfoSize().width(1024).height(1024))
                .coordinates(new TileCoordinates().z(ZOOM).x(TILE_X).y(TILE_Y))
                .build())
        .detectedObjects(
            List.of(
                DetectedObject.builder()
                    .detectedTileId("detectedTileId")
                    .detectedObjectType(
                        DetectableObjectType.builder().detectableType(PARKING).build())
                    .computedConfidence(0.9)
                    .feature(
                        Feature.builder()
                            .id("feature_id")
                            .zoom(ZOOM)
                            .geometry(
                                Feature.FeatureGeometry.builder()
                                    .geometryType(MULTI_POLYGON)
                                    .actualInstanceStringValue(
                                        """
{
  "type": "MultiPolygon",
  "coordinates": [ [ [[200, 200], [200, 800], [800, 800], [800, 200], [200, 200]] ] ]
}""")
                                    .build())
                            .build())
                    .build()))
        .build();
  }
}
