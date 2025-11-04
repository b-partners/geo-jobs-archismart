package app.bpartners.geojobs.model.geometry;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

@AllArgsConstructor
public class GeometryDiff implements BiFunction<Geometry, Geometry, MultiPolygon> {

  private final double minAreaPerPolygon;

  @Override
  public MultiPolygon apply(Geometry g1, Geometry g2) {
    var nonFilteredDiff = g1.difference(g2);
    Set<Polygon> filtered = new HashSet<>();
    for (int n = 0; n < nonFilteredDiff.getNumGeometries(); n++) {
      var nthGeometry = nonFilteredDiff.getGeometryN(n);
      if (nthGeometry.getArea() > minAreaPerPolygon) {
        filtered.add((Polygon) nthGeometry);
      }
    }
    return geometryFactory.createMultiPolygon(filtered.stream().toArray(Polygon[]::new));
  }
}
