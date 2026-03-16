package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.LINE;
import static app.bpartners.geojobs.service.geojson.GeoJson.fromFeatures;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.endpoint.rest.model.TileInfoSize;
import app.bpartners.geojobs.endpoint.rest.postprocessing.DetectionBoundaryMerger;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.geojson.GeoJsonConverter;
import app.bpartners.geojobs.service.geojson.GeoJsonMapper;
import app.bpartners.geojobs.service.geojson.GeoJsonMultiPolygonCorrector;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("TODO: flaky test on CI")
class GeoJsonConverterTest {
  private final GeoJsonMapper mapper = new GeoJsonMapper(new GeoJsonMultiPolygonCorrector());
  private final DetectionBoundaryMerger merger = new DetectionBoundaryMerger();
  private final GeoJsonConverter subject = new GeoJsonConverter(mapper, merger);

  @Test
  void convert_detected_tile_to_geojson_without_feature_collections()
      throws IOException, URISyntaxException {
    var detectedTiles = List.of(detectedTile());

    var features =
        subject.convert(detectedTiles).getGeoFeatures().stream()
            .map(LatLonPolygon::latLon)
            .map(LatLonPolygon::toGeoFeature)
            .toList();
    var actual = fromFeatures(features);

    var expectedURI = Paths.get(getClass().getResource("/geometry/detected-tile.geojson").toURI());
    var expected = JsonParser.parseString(Files.readString(expectedURI));
    assertEquals(expected, JsonParser.parseString(actual.getStringValue()));
  }

  private static DetectedTile detectedTile() {
    return DetectedTile.builder()
        .tile(
            Tile.builder()
                .size(new TileInfoSize().width(1024).height(1024))
                .coordinates(new TileCoordinates().z(20).x(539081).y(367698))
                .build())
        .detectedObjects(
            List.of(
                DetectedObject.builder()
                    .detectedTileId("detectedTileId")
                    .detectedObjectType(DetectableObjectType.builder().detectableType(LINE).build())
                    .computedConfidence(0.9)
                    .feature(
                        Feature.builder()
                            .id("feature_id")
                            .zoom(20)
                            .geometry(
                                Feature.FeatureGeometry.builder()
                                    .geometryType(MULTI_POLYGON)
                                    .actualInstanceStringValue(
                                        """
{
        "type": "MultiPolygon",
        "coordinates": [ [\s
        [[30, 10], [10, 20], [20, 40], [40, 40], [20, 10]],\
        [[20, 20], [25, 30], [15, 30], [20, 20]],\
        [[]]
 ] ] }""")
                                    .build())
                            .build())
                    .build()))
        .build();
  }
}
