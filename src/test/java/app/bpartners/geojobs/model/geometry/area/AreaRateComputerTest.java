package app.bpartners.geojobs.model.geometry.area;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import java.math.BigDecimal;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

public abstract class AreaRateComputerTest {
  protected GeometryFactory geometryFactory = new GeometryFactory();

  protected Polygon createSquare(double size) {
    return geometryFactory.createPolygon(
        new Coordinate[] {
          new Coordinate(0, 0),
          new Coordinate(size, 0),
          new Coordinate(size, size),
          new Coordinate(0, size),
          new Coordinate(0, 0)
        });
  }

  protected DetectedObject createDetectedObject(
      org.locationtech.jts.geom.Polygon polygon,
      app.bpartners.geojobs.repository.model.detection.DetectableType type) {

    DetectedObject detectedObject = org.mockito.Mockito.mock(DetectedObject.class);
    Feature restFeature = new Feature();
    app.bpartners.geojobs.endpoint.rest.model.Polygon restPolygon =
        new app.bpartners.geojobs.endpoint.rest.model.Polygon();

    List<List<List<BigDecimal>>> coordinates = new java.util.ArrayList<>();
    List<List<java.math.BigDecimal>> ring = new java.util.ArrayList<>();
    for (org.locationtech.jts.geom.Coordinate coord : polygon.getCoordinates()) {
      ring.add(
          List.of(java.math.BigDecimal.valueOf(coord.x), java.math.BigDecimal.valueOf(coord.y)));
    }
    coordinates.add(ring);
    restPolygon.setCoordinates(coordinates);
    restPolygon.setType(app.bpartners.geojobs.endpoint.rest.model.Polygon.TypeEnum.POLYGON);

    restFeature.setGeometry(new FeatureGeometry(restPolygon));

    org.mockito.Mockito.when(detectedObject.getFeature()).thenReturn(restFeature);
    org.mockito.Mockito.when(detectedObject.getDetectableObjectType()).thenReturn(type);

    return detectedObject;
  }
}
