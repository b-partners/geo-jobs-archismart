package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeometryTiledValidator implements Function<Object, Boolean> {
  private final GeometryConverter geometryConverter;
  private final TileMultiPolygonFrame tileMultiPolygonFrame;

  @Override
  public Boolean apply(Object geometry) {
    switch (geometry) {
      case Point ignored -> {
        return true;
      }
      case Polygon polygon -> {
        var providedMultiPolygon = geometryConverter.apply(List.of(polygon.getCoordinates()));
        return checkMultiPolygonContainedInFrame(providedMultiPolygon);
      }
      case MultiPolygon multiPolygon -> {
        var providedMultiPolygon = geometryConverter.apply(multiPolygon.getCoordinates());
        return checkMultiPolygonContainedInFrame(providedMultiPolygon);
      }
      case org.locationtech.jts.geom.MultiPolygon jtsMultiPolygon -> {
        return checkMultiPolygonContainedInFrame(jtsMultiPolygon);
      }
      default -> throw new IllegalArgumentException("Unsupported geometry type: " + geometry);
    }
  }

  private boolean checkMultiPolygonContainedInFrame(
      org.locationtech.jts.geom.MultiPolygon providedMultiPolygon) {
    var centroid = geometryConverter.centroidFromGeometry(providedMultiPolygon);
    var longitude = centroid.getFirst();
    var latitude = centroid.getLast();

    var optionalMultiPolygonFrame = tileMultiPolygonFrame.apply(longitude, latitude);
    return optionalMultiPolygonFrame.isPresent()
        && optionalMultiPolygonFrame.get().contains(providedMultiPolygon);
  }
}
