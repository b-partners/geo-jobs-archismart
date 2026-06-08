package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.geometry.MultiPolygonObjectType;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.springframework.stereotype.Component;

@Component
public class PolygonObjectTypeConverter {

  public List<MultiPolygonObjectType> convertFrom(
      Collection<PolygonObjectType> polygonObjectTypes) {
    var groupObjectType =
        polygonObjectTypes.stream().collect(Collectors.groupingBy(PolygonObjectType::objectType));
    return groupObjectType.entrySet().stream()
        .map(
            entry -> {
              var detectableType = entry.getKey();
              var polygons = entry.getValue().stream().map(PolygonObjectType::polygon).toList();
              var geometryMerged = UnaryUnionOp.union(polygons);
              if (geometryMerged instanceof Polygon polygonMerged) {
                var multiPolygon =
                    geometryFactory.createMultiPolygon(new Polygon[] {polygonMerged});
                return new MultiPolygonObjectType(multiPolygon, detectableType);
              } else if (geometryMerged instanceof MultiPolygon multiPolygonMerged) {
                return new MultiPolygonObjectType(multiPolygonMerged, detectableType);
              }
              throw new IllegalStateException(
                  "Unable to convert PolygonObjectType into MultiPolygonObjectType");
            })
        .toList();
  }
}
