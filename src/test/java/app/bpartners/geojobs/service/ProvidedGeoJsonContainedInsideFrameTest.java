package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.gouv.fr.rnb.BuildingApi;
import app.bpartners.geojobs.service.tiling.TileFinder;
import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class ProvidedGeoJsonContainedInsideFrameTest {

  GeometryConverter geometryConverter = new GeometryConverter(new BuildingApi(), null);
  TileMultiPolygonFrame tileMultiPolygonFrame =
      new TileMultiPolygonFrame(new TileFinder(), geometryConverter);

  @Test
  void check_centroid_and_frame() {
    var restMultiPolygon =
        new MultiPolygon()
            .coordinates(
                List.of(
                    List.of(
                        List.of(
                            List.of(BigDecimal.valueOf(-0.2498), BigDecimal.valueOf(46.6517)),
                            List.of(BigDecimal.valueOf(-0.2489), BigDecimal.valueOf(46.6517)),
                            List.of(BigDecimal.valueOf(-0.2489), BigDecimal.valueOf(46.6523)),
                            List.of(BigDecimal.valueOf(-0.2498), BigDecimal.valueOf(46.6523)),
                            List.of(BigDecimal.valueOf(-0.2498), BigDecimal.valueOf(46.6517))))));
    var jtsMultiPolygonProvided = geometryConverter.apply(restMultiPolygon.getCoordinates());
    var centroidCoordinates = geometryConverter.centroidFromGeometry(jtsMultiPolygonProvided);
    var longitude = centroidCoordinates.getFirst();
    var latitude = centroidCoordinates.getLast();
    var jtsMultipolygonFrame = tileMultiPolygonFrame.apply(longitude, latitude).orElseThrow();

    var providedPolygon = geometryConverter.writeGeometryAsString(jtsMultiPolygonProvided);
    var frame = geometryConverter.writeGeometryAsString(jtsMultipolygonFrame);

    log.info("centroid : {}", centroidCoordinates);
    log.info("provided polygon: {}", providedPolygon);
    log.info("frame: {}", frame);

    assertNotNull(providedPolygon);
    assertNotNull(frame);
    assertFalse(jtsMultipolygonFrame.contains(jtsMultiPolygonProvided));
  }
}
