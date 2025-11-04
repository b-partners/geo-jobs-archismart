package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Component;

@Component
public class FeatureConverter {

  public app.bpartners.geojobs.endpoint.rest.model.MultiPolygon fromJtsMultiPolygon(
      MultiPolygon jtsMultiPolygon) {
    List<List<List<List<BigDecimal>>>> coordinates = new ArrayList<>();

    for (int i = 0; i < jtsMultiPolygon.getNumGeometries(); i++) {
      Polygon polygon = (Polygon) jtsMultiPolygon.getGeometryN(i);
      List<List<List<BigDecimal>>> polygonCoords = new ArrayList<>();

      polygonCoords.add(convertCoordinates(polygon.getExteriorRing().getCoordinates()));

      for (int j = 0; j < polygon.getNumInteriorRing(); j++) {
        polygonCoords.add(convertCoordinates(polygon.getInteriorRingN(j).getCoordinates()));
      }
      coordinates.add(polygonCoords);
    }

    return new app.bpartners.geojobs.endpoint.rest.model.MultiPolygon()
        .type(MULTI_POLYGON)
        .coordinates(coordinates);
  }

  private List<List<BigDecimal>> convertCoordinates(Coordinate[] coords) {
    List<List<BigDecimal>> ring = new ArrayList<>();
    for (Coordinate coord : coords) {
      List<BigDecimal> point =
          Arrays.asList(BigDecimal.valueOf(coord.x), BigDecimal.valueOf(coord.y));
      ring.add(point);
    }
    return ring;
  }
}
